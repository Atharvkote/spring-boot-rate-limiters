package com.example.ratelimiter.limiters;

import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.enums.ClientType;
import com.example.ratelimiter.limiters.algos.FixedWindowAlgorithm;
import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import com.example.ratelimiter.limiters.records.RateLimitResult;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedWindowAlgorithmTest {

    private FixedWindowAlgorithm algorithm;
    @Mock
    private RateLimitStore store;
    private static final String KEY = "rl:general:ip:1.1.1.1";
    private final RateLimitPolicy policy = new RateLimitPolicy(100, Duration.ofSeconds(60), AlgorithmType.FIXED_WINDOW,
            ClientType.IP);

    @BeforeEach
    void setUp() {
        algorithm = new FixedWindowAlgorithm(store);
    }

    @Test
    @DisplayName("Under limit → allowed")
    void underLimit() {
        when(store.increment(eq(KEY), any())).thenReturn(1L);
        when(store.getTtl(KEY)).thenReturn(58L);
        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isTrue();
        assertThat(r.remaining()).isEqualTo(99);
    }

    @Test
    @DisplayName("At limit → allowed with 0 remaining")
    void atLimit() {
        when(store.increment(eq(KEY), any())).thenReturn(100L);
        when(store.getTtl(KEY)).thenReturn(30L);
        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isTrue();
        assertThat(r.remaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("Over limit → rejected")
    void overLimit() {
        when(store.increment(eq(KEY), any())).thenReturn(101L);
        when(store.getTtl(KEY)).thenReturn(45L);
        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.allowed()).isFalse();
        assertThat(r.remaining()).isEqualTo(0);
        assertThat(r.retryAfterSeconds()).isEqualTo(45);
    }

    @Test
    @DisplayName("Negative TTL treated as 0")
    void negativeTtl() {
        when(store.increment(eq(KEY), any())).thenReturn(101L);
        when(store.getTtl(KEY)).thenReturn(-1L);
        RateLimitResult r = algorithm.check(KEY, policy);
        assertThat(r.resetAfterSeconds()).isEqualTo(0);
    }
}
