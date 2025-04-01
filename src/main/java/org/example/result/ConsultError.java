package org.example.result;

import lombok.AllArgsConstructor;
import lombok.Getter; /***
 * 用户有多种支付方式（余额、红包、优惠券，代金券等），假如每种支付方式需要通过实时调用远程服务获取可用性。
 * 在外部资源环境不变情况下，请设计程序以最短响应时间获得尽可能多的可用支付方式列表。
 * 假定支付方式可用性咨询服务接口定义：PaymentRemoteService
 * 接口方法：ConsultResult isEnabled(String paymentType);
 * 返回结果：
 */

@AllArgsConstructor
@Getter
public enum ConsultError {
    NotDefine("NotDefine", "未定义当前的支付类型"),
    NotNotFoundImpl("NotNotFoundImpl","未找到当前支付类型的实现"),
    CircuitBreak("CircuitBreak","当前支付类型处于熔断状态"),
    RateLimited("RateLimited", "被限流"),
    UnknownError("UnknownError", "未知错误"),
    Interrupt("Interrupt", "被中断"),
    NotSchedule("NotSchedule", "未被调度"),
    TimeOut("TimeOut", "超时");

    private final String errorCode;
    private final String errorMessage;
}
