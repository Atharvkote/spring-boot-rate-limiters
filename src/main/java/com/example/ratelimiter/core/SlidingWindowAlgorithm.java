package com.example.ratelimiter.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlidingWindowAlgorithm implements RateLimitAlgorithm {

    private final RateLimitStore store;

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy) {
        long now = System.currentTimeMillis();
        long limit = policy.limit();
        long windowSec = policy.window().toSeconds();

        long result = store.checkSlidingWindow(key, policy.window(), policy.limit(), now);

        if (result == -1) {
            // Rejected
            Long oldestScore = store.getOldestZSetScore(key);
            long retryAfterSec = windowSec;
            if (oldestScore != null) {
                long elapsed = now - oldestScore;
                long remainMs = (windowSec * 1000) - elapsed;
                retryAfterSec = Math.max(1, (long) Math.ceil(remainMs / 1000.0));
            }
            log.debug("SlidingWindow rejected key={} retryAfter={}", key, retryAfterSec);
            return new RateLimitResult(false, limit, 0, retryAfterSec, retryAfterSec);
        } else {
            // Allowed
            long count = result;
            long remaining = Math.max(0, limit - count);
            Long oldestScore = store.getOldestZSetScore(key);
            long resetAfterSec = windowSec;
            if (oldestScore != null) {
                long elapsed = now - oldestScore;
                long remainMs = (windowSec * 1000) - elapsed;
                resetAfterSec = Math.max(1, (long) Math.ceil(remainMs / 1000.0));
            }
            log.debug("SlidingWindow allowed key={} remaining={} resetAfter={}", key, remaining, resetAfterSec);
            return new RateLimitResult(true, limit, remaining, 0, resetAfterSec);
        }
    }
}
