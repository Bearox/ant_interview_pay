package org.example.circuit_breaker;

public class MockCircuitBreak implements CircuitBreak {
    /**
     * @param key
     * @return
     */
    @Override
    public boolean isAllowed(String key) {
        return true;
    }

    /**
     * @param key
     * @param success
     */
    @Override
    public void reportStatus(String key, boolean success) {

    }
}
