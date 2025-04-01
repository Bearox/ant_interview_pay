package org.example.rate_limit;

public class MockRateLimiter implements RateLimiter {
    @Override
    public boolean isAllowed(String key) {
        return true;
    }
}
