package com.example.ratelimiter.core;

public interface RateLimitAlgorithm {

    RateLimitResult check(String key, RateLimitPolicy policy);
}
