package com.example.ratelimiter.limiters;

import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.limiters.algos.RateLimitAlgorithm;
import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import com.example.ratelimiter.limiters.records.ClientContext;
import com.example.ratelimiter.limiters.records.RateLimitResult;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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
