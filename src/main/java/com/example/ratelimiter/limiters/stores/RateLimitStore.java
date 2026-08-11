package com.example.ratelimiter.limiters.stores;

import java.time.Duration;

public interface RateLimitStore {

    long increment(String key, Duration window);

    long getTtl(String key);

    long checkSlidingWindow(String key, Duration window, int limit, long now);

    Long getOldestZSetScore(String key);

    long checkTokenBucket(String key, int limit, double refillRate, long now, Duration window);

    Double getHashDouble(String key, String field);
}
