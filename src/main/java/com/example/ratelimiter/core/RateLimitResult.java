package com.example.ratelimiter.core;

public record RateLimitResult(
    boolean allowed,
    long limit,
    long remaining,
    long retryAfterSeconds,
    long resetAfterSeconds
) {}
