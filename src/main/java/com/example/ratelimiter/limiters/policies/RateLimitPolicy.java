package com.example.ratelimiter.limiters.policies;

import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.enums.ClientType;
import com.example.ratelimiter.config.properties.RateLimiterProperties;

import java.time.Duration;

public record RateLimitPolicy(
    int limit,
    Duration window,
    AlgorithmType algorithm,
    ClientType clientType
) {
    public static RateLimitPolicy from(RateLimiterProperties.PolicyConfig config) {
        return new RateLimitPolicy(
                config.getLimit(),
                config.getWindow(),
                config.getAlgorithm(),
                config.getClientType()
        );
    }
}
