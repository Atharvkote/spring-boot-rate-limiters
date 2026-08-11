package com.example.ratelimiter.limiters.algos;

import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import com.example.ratelimiter.limiters.records.RateLimitResult;
import com.example.ratelimiter.limiters.stores.RateLimitStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedWindowAlgorithm implements RateLimitAlgorithm {

    private final RateLimitStore store;

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy) {
        long count = store.increment(key, policy.window());
        long limit = policy.limit();
        long ttl = store.getTtl(key);

        boolean allowed = count <= limit;
        long remaining = Math.max(0, limit - count);
        long retryAfter = allowed ? 0 : Math.max(ttl, 0);
        long resetAfter = Math.max(ttl, 0);

        log.debug("FixedWindow key={} count={} limit={} allowed={} remaining={}", key, count, limit, allowed,
                remaining);
        return new RateLimitResult(allowed, limit, remaining, retryAfter, resetAfter);
    }
}
