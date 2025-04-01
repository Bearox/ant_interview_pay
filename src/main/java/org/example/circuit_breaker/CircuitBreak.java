package org.example.circuit_breaker;

public interface CircuitBreak {
    // @return 是否允许执行 false 表示当前已经被熔断
    boolean isAllowed(String key);
    // @param  上报当前的执行结果，根据上报结果会修改熔断器的状态
    void reportStatus(String key, boolean success);
}
