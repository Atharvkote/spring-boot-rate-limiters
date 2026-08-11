package com.example.ratelimiter.core;

import com.example.ratelimiter.config.AlgorithmType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Single concrete rate limiter — replaces the old interface + abstract base + 4 subclasses.
 * Instances are created per-category as Spring beans in {@code RateLimiterConfig}.
 * <p>
 * Key format: {@code rl:{category}:{clientType}:{clientId}}
 * <br>Examples: {@code rl:auth:ip:10.0.0.1}, {@code rl:sensitive:user:123}
 */
@Slf4j
public class RateLimiter {

    private final String category;
    private final RateLimitPolicy policy;
    private final Map<AlgorithmType, RateLimitAlgorithm> algorithms;

    public RateLimiter(String category, RateLimitPolicy policy, Map<AlgorithmType, RateLimitAlgorithm> algorithms) {
        this.category = category;
        this.policy = policy;
        this.algorithms = algorithms;
    }

    public RateLimitResult check(ClientContext context) {
        String key = buildKey(context);
        RateLimitAlgorithm algorithm = algorithms.get(policy.algorithm());
        RateLimitResult result = algorithm.check(key, policy);

        log.info("Rate limit check category={} clientType={} clientId={} method={} path={} allowed={} remaining={}",
                category, context.clientType(), context.clientId(),
                context.httpMethod(), context.requestPath(),
                result.allowed(), result.remaining());

        return result;
    }

    public String getCategory() {
        return category;
    }

    public RateLimitPolicy getPolicy() {
        return policy;
    }

    private String buildKey(ClientContext context) {
        return "rl:" + category + ":"
                + policy.clientType().name().toLowerCase() + ":"
                + context.clientId();
    }
}
