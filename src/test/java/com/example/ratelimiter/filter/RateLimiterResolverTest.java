package com.example.ratelimiter.filter;

import com.example.ratelimiter.core.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterResolverTest {

    private RateLimiterResolver resolver;
    @Mock private RateLimiter auth, upload, sensitive, general;
    @Mock private HttpServletRequest request;

    @BeforeEach
    void setUp() { resolver = new RateLimiterResolver(auth, upload, sensitive, general); }

    @Test @DisplayName("/api/auth/login → auth")
    void authRoute() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        assertThat(resolver.resolve(request)).isSameAs(auth);
    }

    @Test @DisplayName("/api/upload → upload")
    void uploadRoute() {
        when(request.getRequestURI()).thenReturn("/api/upload");
        assertThat(resolver.resolve(request)).isSameAs(upload);
    }

    @Test @DisplayName("/api/payment → sensitive")
    void paymentRoute() {
        when(request.getRequestURI()).thenReturn("/api/payment");
        assertThat(resolver.resolve(request)).isSameAs(sensitive);
    }

    @Test @DisplayName("DELETE /api/users/5 → sensitive")
    void deleteUserRoute() {
        when(request.getRequestURI()).thenReturn("/api/users/5");
        when(request.getMethod()).thenReturn("DELETE");
        assertThat(resolver.resolve(request)).isSameAs(sensitive);
    }

    @Test @DisplayName("GET /api/users/5 → general (not DELETE)")
    void getUserRoute() {
        when(request.getRequestURI()).thenReturn("/api/users/5");
        when(request.getMethod()).thenReturn("GET");
        assertThat(resolver.resolve(request)).isSameAs(general);
    }

    @Test @DisplayName("/api/test → general")
    void fallback() {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        assertThat(resolver.resolve(request)).isSameAs(general);
    }
}
