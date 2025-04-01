package org.example.rate_limit;

public interface RateLimiter {
    boolean isAllowed(String key);
}
