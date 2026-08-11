package com.example.ratelimiter.limiters;

import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.enums.ClientType;
import com.example.ratelimiter.config.properties.RateLimiterProperties;
import com.example.ratelimiter.limiters.algos.RateLimitAlgorithm;
import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import com.example.ratelimiter.limiters.records.ClientContext;
import com.example.ratelimiter.limiters.records.RateLimitResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimiterTest {

    @Test
    @DisplayName("Key format: rl:{category}:{clientType}:{clientId}")
    void keyFormat() {
        var algo = mock(RateLimitAlgorithm.class);
        var policy = new RateLimitPolicy(10, Duration.ofSeconds(60), AlgorithmType.FIXED_WINDOW, ClientType.IP);
        var limiter = new RateLimiter("auth", policy, Map.of(AlgorithmType.FIXED_WINDOW, algo));
        var ctx = new ClientContext("10.0.0.1", ClientType.IP, "10.0.0.1", null, "POST", "/api/auth/login");

        when(algo.check(anyString(), any())).thenReturn(new RateLimitResult(true, 10, 9, 0, 58));
        limiter.check(ctx);

        verify(algo).check(eq("rl:auth:ip:10.0.0.1"), any());
    }

    @Test
    @DisplayName("Policy.from() maps config correctly")
    void policyFromConfig() {
        var cfg = new RateLimiterProperties.PolicyConfig();
        cfg.setLimit(20);
        cfg.setWindow(Duration.ofSeconds(30));
        cfg.setAlgorithm(AlgorithmType.FIXED_WINDOW);
        cfg.setClientType(ClientType.USER);

        RateLimitPolicy p = RateLimitPolicy.from(cfg);
        assertThat(p.limit()).isEqualTo(20);
        assertThat(p.window()).isEqualTo(Duration.ofSeconds(30));
        assertThat(p.algorithm()).isEqualTo(AlgorithmType.FIXED_WINDOW);
        assertThat(p.clientType()).isEqualTo(ClientType.USER);
    }
}
