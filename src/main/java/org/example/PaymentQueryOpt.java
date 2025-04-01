package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Data
public class PaymentQueryOpt {
    private boolean noCache;    // 本次查询不走缓存
    private long timeOutMillis; // 超时时间
    private int maxRetryTimes;  // 重试次数

    @Getter
    private final static PaymentQueryOpt defaultOpt = new PaymentQueryOpt();

    // 通过无参构造函数制定默认参数
    public PaymentQueryOpt() {
        noCache = false;
        timeOutMillis = 1000;
        maxRetryTimes = 0;
    }
}
