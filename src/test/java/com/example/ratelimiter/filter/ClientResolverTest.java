package com.example.ratelimiter.filter;

import com.example.ratelimiter.enums.ClientType;
import com.example.ratelimiter.limiters.records.ClientContext;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientResolverTest {

    private final ClientResolver resolver = new ClientResolver();
    @Mock
    private HttpServletRequest request;
    @Mock
    private Principal principal;

    @Test
    @DisplayName("Anonymous → IP client")
    void anonymous() {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        ClientContext ctx = resolver.resolve(request);
        assertThat(ctx.clientType()).isEqualTo(ClientType.IP);
        assertThat(ctx.clientId()).isEqualTo("192.168.1.10");
        assertThat(ctx.userId()).isNull();
    }

    @Test
    @DisplayName("Authenticated → USER client")
    void authenticated() {
        when(request.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("user123");
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/payment");
        ClientContext ctx = resolver.resolve(request);
        assertThat(ctx.clientType()).isEqualTo(ClientType.USER);
        assertThat(ctx.clientId()).isEqualTo("user123");
    }

    @Test
    @DisplayName("Blank principal name → falls back to IP")
    void blankPrincipal() {
        when(request.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("  ");
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        ClientContext ctx = resolver.resolve(request);
        assertThat(ctx.clientType()).isEqualTo(ClientType.IP);
    }
}
