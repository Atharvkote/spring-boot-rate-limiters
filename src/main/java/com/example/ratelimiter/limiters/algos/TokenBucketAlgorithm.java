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
public class TokenBucketAlgorithm implements RateLimitAlgorithm {

    private final RateLimitStore store;

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy) {
        long now = System.currentTimeMillis();
        int limit = policy.limit();
        double refillRate = (double) limit / policy.window().toSeconds();

        long result = store.checkTokenBucket(key, limit, refillRate, now, policy.window());

        if (result == -1) {
            // Rejected
            Double tokensVal = store.getHashDouble(key, "tokens");
            double currentTokens = tokensVal != null ? tokensVal : 0.0;

            long retryAfterSec = Math.max(1, (long) Math.ceil((1.0 - currentTokens) / refillRate));
            long resetAfterSec = Math.max(1, (long) Math.ceil((limit - currentTokens) / refillRate));

            log.debug("TokenBucket rejected key={} tokens={} retryAfter={}", key, currentTokens, retryAfterSec);
            return new RateLimitResult(false, limit, 0, retryAfterSec, resetAfterSec);
        } else {
            // Allowed
            long remaining = result;
            long resetAfterSec = Math.max(1, (long) Math.ceil((limit - remaining) / refillRate));

            log.debug("TokenBucket allowed key={} remaining={} resetAfter={}", key, remaining, resetAfterSec);
            return new RateLimitResult(true, limit, remaining, 0, resetAfterSec);
        }
    }
}
