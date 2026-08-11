package com.example.ratelimiter.limiters.algos;

import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import com.example.ratelimiter.limiters.records.RateLimitResult;

public interface RateLimitAlgorithm {
    RateLimitResult check(String key, RateLimitPolicy policy);
}
