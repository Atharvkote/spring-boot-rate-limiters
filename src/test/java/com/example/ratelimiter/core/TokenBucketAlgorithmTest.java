package com.example.ratelimiter.core;

import com.example.ratelimiter.config.AlgorithmType;
import com.example.ratelimiter.config.ClientType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBucketAlgorithmTest {

    private TokenBucketAlgorithm algorithm;
    @Mock private RateLimitStore store;
    private static final String KEY = "rl:general:ip:1.1.1.1";
    // 10 tokens over 10 seconds = refill rate of 1 token per second
    private final RateLimitPolicy policy = new RateLimitPolicy(10, Duration.ofSeconds(10), AlgorithmType.TOKEN_BUCKET, ClientType.IP);

    @BeforeEach
    void setUp() { algorithm = new TokenBucketAlgorithm(store); }

    @Test @DisplayName("Under limit → allowed")
    void underLimit() {
        when(store.checkTokenBucket(eq(KEY), eq(10), eq(1.0), anyLong(), any())).thenReturn(8L);

        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isTrue();
        assertThat(r.remaining()).isEqualTo(8);
        assertThat(r.resetAfterSeconds()).isEqualTo(2); // (10 capacity - 8 remaining) / 1.0 = 2s
    }

    @Test @DisplayName("Over limit → retryAfter calculated based on current tokens")
    void rejected() {
        when(store.checkTokenBucket(eq(KEY), eq(10), eq(1.0), anyLong(), any())).thenReturn(-1L);
        // Current tokens left in the bucket: 0.2 tokens
        when(store.getHashDouble(KEY, "tokens")).thenReturn(0.2);

        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isFalse();
        assertThat(r.remaining()).isEqualTo(0);
        // Need to wait until 1.0 tokens are available. Current is 0.2. Remaining needed = 0.8.
        // With refill rate of 1.0/sec, wait time is ceil(0.8 / 1.0) = 1s.
        assertThat(r.retryAfterSeconds()).isEqualTo(1L);
        // Reset to full capacity (10 tokens) from 0.2: (10 - 0.2) / 1.0 = 9.8. ceil = 10s.
        assertThat(r.resetAfterSeconds()).isEqualTo(10L);
    }
}
