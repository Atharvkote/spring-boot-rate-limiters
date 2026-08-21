package com.example.ratelimiter.core;

import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.enums.ClientType;
import com.example.ratelimiter.limiters.records.RateLimitResult;
import com.example.ratelimiter.limiters.algos.SlidingWindowAlgorithm;
import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import com.example.ratelimiter.limiters.stores.RateLimitStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlidingWindowAlgorithmTest {

    private SlidingWindowAlgorithm algorithm;
    @Mock private RateLimitStore store;
    private static final String KEY = "rl:general:ip:1.1.1.1";
    private final RateLimitPolicy policy = new RateLimitPolicy(5, Duration.ofSeconds(10), AlgorithmType.SLIDING_WINDOW, ClientType.IP);

    @BeforeEach
    void setUp() { algorithm = new SlidingWindowAlgorithm(store); }

    @Test @DisplayName("Under limit → allowed")
    void underLimit() {
        when(store.checkSlidingWindow(eq(KEY), any(), eq(5), anyLong())).thenReturn(3L);
        when(store.getOldestZSetScore(KEY)).thenReturn(System.currentTimeMillis() - 2000L); // 2s ago

        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isTrue();
        assertThat(r.remaining()).isEqualTo(2);
        assertThat(r.resetAfterSeconds()).isLessThanOrEqualTo(8);
    }

    @Test @DisplayName("Rejected → retryAfter calculated from oldest score")
    void rejected() {
        long now = System.currentTimeMillis();
        when(store.checkSlidingWindow(eq(KEY), any(), eq(5), anyLong())).thenReturn(-1L);
        when(store.getOldestZSetScore(KEY)).thenReturn(now - 4000L); // oldest was 4s ago

        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isFalse();
        assertThat(r.remaining()).isEqualTo(0);
        // Window is 10s, oldest is 4s ago, next free slot is in 6s
        assertThat(r.retryAfterSeconds()).isEqualTo(6L);
        assertThat(r.resetAfterSeconds()).isEqualTo(6L);
    }

    @Test @DisplayName("Rejected with null oldest score → defaults to window size")
    void rejectedNoOldest() {
        when(store.checkSlidingWindow(eq(KEY), any(), eq(5), anyLong())).thenReturn(-1L);
        when(store.getOldestZSetScore(KEY)).thenReturn(null);

        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isFalse();
        assertThat(r.retryAfterSeconds()).isEqualTo(10L);
    }
}
