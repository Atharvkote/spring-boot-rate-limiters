package com.example.ratelimiter.filter;

import com.example.ratelimiter.config.ClientType;
import com.example.ratelimiter.config.FailMode;
import com.example.ratelimiter.config.RateLimiterProperties;
import com.example.ratelimiter.core.ClientContext;
import com.example.ratelimiter.core.RateLimitResult;
import com.example.ratelimiter.core.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private RateLimiterProperties props;
    @Mock private ClientResolver clientResolver;
    @Mock private RateLimiterResolver limiterResolver;
    @Mock private RateLimiter limiter;
    @Mock private FilterChain chain;
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;

    @BeforeEach
    void setUp() {
        props = new RateLimiterProperties();
        props.setEnabled(true);
        props.setFailMode(FailMode.OPEN);
        filter = new RateLimitFilter(props, clientResolver, limiterResolver, new ObjectMapper(), new SimpleMeterRegistry());
        req = new MockHttpServletRequest();
        req.setRequestURI("/api/test");
        res = new MockHttpServletResponse();
    }

    @Test @DisplayName("Allowed → chain continues + headers set")
    void allowed() throws Exception {
        var ctx = new ClientContext("1.1.1.1", ClientType.IP, "1.1.1.1", null, "GET", "/api/test");
        when(clientResolver.resolve(any())).thenReturn(ctx);
        when(limiterResolver.resolve(any())).thenReturn(limiter);
        when(limiter.check(ctx)).thenReturn(new RateLimitResult(true, 100, 99, 0, 58));
        when(limiter.getCategory()).thenReturn("general");

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
    }

    @Test @DisplayName("Rejected → 429 + Retry-After")
    void rejected() throws Exception {
        var ctx = new ClientContext("1.1.1.1", ClientType.IP, "1.1.1.1", null, "GET", "/api/test");
        when(clientResolver.resolve(any())).thenReturn(ctx);
        when(limiterResolver.resolve(any())).thenReturn(limiter);
        when(limiter.check(ctx)).thenReturn(new RateLimitResult(false, 100, 0, 45, 45));
        when(limiter.getCategory()).thenReturn("general");

        filter.doFilterInternal(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("45");
    }

    @Test @DisplayName("Disabled → passes through")
    void disabled() throws Exception {
        props.setEnabled(false);
        filter = new RateLimitFilter(props, clientResolver, limiterResolver, new ObjectMapper(), new SimpleMeterRegistry());
        filter.doFilterInternal(req, res, chain);
        verify(chain).doFilter(req, res);
        verifyNoInteractions(clientResolver);
    }

    @Test @DisplayName("Fail-OPEN → allows on error")
    void failOpen() throws Exception {
        when(clientResolver.resolve(any())).thenThrow(new RuntimeException("Redis down"));
        filter.doFilterInternal(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test @DisplayName("Fail-CLOSED → rejects on error")
    void failClosed() throws Exception {
        props.setFailMode(FailMode.CLOSED);
        filter = new RateLimitFilter(props, clientResolver, limiterResolver, new ObjectMapper(), new SimpleMeterRegistry());
        when(clientResolver.resolve(any())).thenThrow(new RuntimeException("Redis down"));
        filter.doFilterInternal(req, res, chain);
        verify(chain, never()).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test @DisplayName("Actuator skipped")
    void actuatorSkipped() {
        req.setRequestURI("/actuator/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }
}
