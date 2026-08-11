package com.example.ratelimiter.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> fixedWindowScript;
    private final RedisScript<Long> slidingWindowScript;
    private final RedisScript<Long> tokenBucketScript;

    @Override
    public long increment(String key, Duration window) {
        Long count = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(key),
                String.valueOf(window.getSeconds())
        );
        log.debug("Redis INCR key={} window={}s count={}", key, window.getSeconds(), count);
        return count != null ? count : 0;
    }

    @Override
    public long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    @Override
    public long checkSlidingWindow(String key, Duration window, int limit, long now) {
        Long count = redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(key),
                String.valueOf(window.getSeconds()),
                String.valueOf(limit),
                String.valueOf(now)
        );
        log.debug("Redis SlidingWindow key={} count={}", key, count);
        return count != null ? count : -1;
    }

    @Override
    public Long getOldestZSetScore(String key) {
        Set<String> oldest = redisTemplate.opsForZSet().range(key, 0, 0);
        if (oldest != null && !oldest.isEmpty()) {
            String member = oldest.iterator().next();
            try {
                String[] parts = member.split("_");
                return Long.parseLong(parts[0]);
            } catch (Exception ex) {
                log.error("Failed to parse timestamp from member={}", member, ex);
            }
        }
        return null;
    }

    @Override
    public long checkTokenBucket(String key, int limit, double refillRate, long now, Duration window) {
        Long remaining = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(refillRate),
                String.valueOf(now),
                String.valueOf(window.getSeconds())
        );
        log.debug("Redis TokenBucket key={} remaining={}", key, remaining);
        return remaining != null ? remaining : -1;
    }

    @Override
    public Double getHashDouble(String key, String field) {
        Object val = redisTemplate.opsForHash().get(key, field);
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (Exception ex) {
                log.error("Failed to parse double from key={} field={} val={}", key, field, val, ex);
            }
        }
        return null;
    }
}
