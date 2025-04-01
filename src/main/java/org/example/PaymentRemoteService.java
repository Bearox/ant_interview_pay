package org.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.AllArgsConstructor;
import org.example.circuit_breaker.CircuitBreak;
import org.example.metric.Metric;
import org.example.pay.PaymentBalanceImp;
import org.example.pay.PaymentRedPocketImpl;
import org.example.pay.PaymentService;
import org.example.pay.PaymentTypeEnum;
import org.example.rate_limit.RateLimiter;
import org.example.result.ConstResult;
import org.example.result.ConsultError;
import org.example.result.ConsultResult;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 *  * 用户有多种支付方式（余额、红包、优惠券，代金券等），假如每种支付方式需要通过实时调用远程服务获取可用性。
 *  * 在外部资源环境不变情况下，请设计程序以最短响应时间获得尽可能多的可用支付方式列表。
 */
@AllArgsConstructor
public class PaymentRemoteService {
    private CircuitBreak circuitBreak; // 熔断器
    private Metric metric;             // 埋点上报
    private RateLimiter rateLimiter;   // 限流

    // 定义通用线程池 - TODO 根据实际情况对参数进行调整
    private static final ExecutorService executor = new ThreadPoolExecutor(
            4,
            10,
            10L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    public static void close(){
        executor.shutdown();
    }

    private static final Map<PaymentTypeEnum, PaymentService> serviceMap = new HashMap<>(){{
        put(PaymentTypeEnum.Balance, new PaymentBalanceImp());
        put(PaymentTypeEnum.RedPocket, new PaymentRedPocketImpl());

    }};

    // TODO - 配置化，支持热更新，demo中写死
    // ------  重试相关参数
    private static final long maxDurationMillis = 1000; // 最大重试次数
    private static final long retryDelayMillis = 100; // 每次重试间隔
    private static final int  retryTimes = 3; // 重试次数
    // ------  缓存相关参数
    private static final long cacheExpireMillis = 1000;
    private static final long refreshMillis = (long) (0.7 * cacheExpireMillis);  // 0.7 可以根据实际场景进行调优
    // ------ 超时配置
    private static final long singleTimeOutMillis = 1000;


    // demo 中简单使用了一个本地缓存，此处需要根据实际场景判断是否需要引入分布式缓存
    private static final Cache<PaymentTypeEnum, ConsultResult> paymentResultCache = Caffeine.
            newBuilder().
            expireAfterWrite(Duration.ofMillis(cacheExpireMillis)).
            refreshAfterWrite(Duration.ofMillis(refreshMillis)).
            maximumSize(100).
            build();

    public ConsultResult isEnabled(String payment) {
        // 1. valid paymentType
        var paymentTypeEnum = PaymentTypeEnum.fromKey(payment);
        if (paymentTypeEnum == PaymentTypeEnum.NotDefine) {
            return  new ConsultResult(false, ConsultError.NotDefine.getErrorCode());
        }
        // 2. 获取对应的支付远程调用方法
        var service = serviceMap.get(paymentTypeEnum);
        if (service == null) {
            return  new ConsultResult(false, ConsultError.NotNotFoundImpl.getErrorCode());
        }
        return wrapIsEnabled(service, PaymentQueryOpt.getDefaultOpt());
    }



    // 获取可用的支付方式列表
    public List<PaymentTypeEnum> getValidPaymentList(PaymentQueryOpt opt) {
        // 为每个支付方式创建异步任务
        Map<PaymentTypeEnum, CompletableFuture<ConsultResult>> futureMap = serviceMap.entrySet().stream().
                collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> CompletableFuture.supplyAsync(() -> wrapIsEnabled(entry.getValue(), opt), executor)
                        .exceptionally(ex -> {
                            // 不同的异常封装为不同的 ConsultResult 返回
                            if (ex instanceof TimeoutException) {
                                return ConstResult.timeOutResult;
                            }
                            return ConstResult.unknownResult;
                        }) // 异常处理为结果
        ));
        List<CompletableFuture<ConsultResult>> futures = futureMap.values().stream().toList();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // 指定总的超时时间
        if (opt.getTimeOutMillis() > 0 ) {
             allFutures.orTimeout(opt.getTimeOutMillis(), TimeUnit.MILLISECONDS);
        }

        var defaultResult = ConstResult.unknownResult;
        try {
            allFutures.get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                defaultResult = ConstResult.timeOutResult;
            }
           System.out.println(e.getMessage());
        }

        // todo - 结果可以加一些日志，方便定位排查问题
        ConsultResult finalDefaultResult = defaultResult;
        return futureMap.entrySet().stream().
                filter(entry -> entry.getValue().getNow(finalDefaultResult).getIsEnable())
                .map(Map.Entry::getKey).toList();

    }

    // 熔断 | 限流 | RPC 请求调用超时配置，更合理的支持方式是在Service Mesh 中通过配置来支持，这里假设 Mesh 没有提供对应的能力
    private ConsultResult wrapIsEnabled(PaymentService paymentService, PaymentQueryOpt opt) {
        // 判断当前是否被熔断 | 熔断过程中采样下放请求，判断服务是否已经恢复
        if (!circuitBreak.isAllowed(paymentService.getPaymentType().getKey())) {
            return ConstResult.circuitBreakResult;
        }
        // 是否被限流
        if (!rateLimiter.isAllowed(paymentService.getPaymentType().getKey())) {
            return ConstResult.limitedResult;
        }

        // 强制不走缓存，查询前删除缓存
        // 高并发的情况下，还是可能会从缓存中读取到结果。这里间隔非常短，理论上不影响。如果是对一致性要求非常高的场景，需要通过事务和锁来支持
        if (opt.isNoCache()) {
            paymentResultCache.invalidate(paymentService.getPaymentType());
        }
        return paymentResultCache.get(paymentService.getPaymentType(), (key) -> {
            var retryPolicy = RetryPolicy.builder()
                    .withMaxDuration(Duration.ofMillis(maxDurationMillis))
                    .withDelay(Duration.ofMillis(retryDelayMillis))
                    .withMaxAttempts(retryTimes)
                    .onFailedAttempt(event -> {
                        // 请求失败上报给熔断器判断是否要触发熔断
                        circuitBreak.reportStatus(paymentService.getPaymentType().getKey(), false);
                        // 打点信息，用于统计对应类型的方法的执行时间、执行结果等信息
                        metric.report();
                    }).build();
            var failsafe = Failsafe.with(retryPolicy);
            try {
                return (ConsultResult) failsafe.getAsync(context -> paymentService.isEnabled()).get();
            } catch (InterruptedException e) {
                return ConstResult.interruptResult;
            } catch (ExecutionException e) {
                return ConstResult.unknownResult;
            }
        });
    }
}
