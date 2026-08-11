package com.example.ratelimiter.core;

import com.example.ratelimiter.config.AlgorithmType;
import com.example.ratelimiter.config.ClientType;
import com.example.ratelimiter.config.RateLimiterProperties;

import java.time.Duration;

/**
 * Immutable policy record — replaces 4 separate policy classes + interface.
 * Built from {@link RateLimiterProperties.PolicyConfig} via the factory method.
 */
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
