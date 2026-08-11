package com.example.ratelimiter.filter;

import com.example.ratelimiter.config.ClientType;
import com.example.ratelimiter.core.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Resolves the client identity from an HTTP request.
 * <p>
 * Strategy:
 * <ol>
 *   <li>If authenticated → {@code user:{principalName}}</li>
 *   <li>Otherwise → {@code ip:{remoteAddr}}</li>
 * </ol>
 * Does <em>not</em> blindly trust {@code X-Forwarded-For}.
 * Configure {@code server.forward-headers-strategy} for trusted proxies.
 */
@Slf4j
@Component
public class ClientResolver {

    public ClientContext resolve(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String ip = request.getRemoteAddr();
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            String userId = principal.getName();
            log.debug("Resolved authenticated client userId={} ip={}", userId, ip);
            return new ClientContext(userId, ClientType.USER, ip, userId, method, path);
        }

        log.debug("Resolved anonymous client ip={}", ip);
        return new ClientContext(ip, ClientType.IP, ip, null, method, path);
    }
}
