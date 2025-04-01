package org.example.result;

public class ConstResult {
    // 定义常量池
    public static final ConsultResult successResult = new ConsultResult(true, "success");
    public static final ConsultResult timeOutResult = new ConsultResult(false, ConsultError.TimeOut.getErrorCode());
    public static final ConsultResult circuitBreakResult = new ConsultResult(false, ConsultError.CircuitBreak.getErrorCode());
    public static final ConsultResult limitedResult = new ConsultResult(false, ConsultError.RateLimited.getErrorCode());
    public static final ConsultResult unknownResult = new ConsultResult(false, ConsultError.UnknownError.getErrorCode());
    public static final ConsultResult interruptResult = new ConsultResult(false, ConsultError.Interrupt.getErrorCode());
}
