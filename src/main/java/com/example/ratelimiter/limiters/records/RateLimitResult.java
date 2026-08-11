package com.example.ratelimiter.limiters.records;

public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds,
        long resetAfterSeconds) {
}
