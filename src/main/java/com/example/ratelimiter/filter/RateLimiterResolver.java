package com.example.ratelimiter.filter;

import com.example.ratelimiter.core.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Maps incoming requests to the correct {@link RateLimiter} by route.
 * <p>
 * Precedence (first match wins):
 * <ol>
 *   <li>AUTH — {@code /api/auth/**}</li>
 *   <li>UPLOAD — {@code /api/upload/**}, {@code /api/files/upload/**}</li>
 *   <li>SENSITIVE — {@code /api/payment/**}, {@code /api/admin/**}, {@code DELETE /api/users/**}</li>
 *   <li>GENERAL — everything else</li>
 * </ol>
 */
@Slf4j
@Component
public class RateLimiterResolver {

    private final List<RouteRule> rules;
    private final RateLimiter generalLimiter;

    public RateLimiterResolver(
            @Qualifier("authRateLimiter") RateLimiter auth,
            @Qualifier("uploadRateLimiter") RateLimiter upload,
            @Qualifier("sensitiveRateLimiter") RateLimiter sensitive,
            @Qualifier("generalRateLimiter") RateLimiter general
    ) {
        this.generalLimiter = general;
        this.rules = new ArrayList<>();
        rules.add(new RouteRule(r -> path(r).startsWith("/api/auth/") || path(r).equals("/api/auth"), auth, "AUTH"));
        rules.add(new RouteRule(r -> path(r).startsWith("/api/upload") || path(r).startsWith("/api/files/upload"), upload, "UPLOAD"));
        rules.add(new RouteRule(r -> path(r).startsWith("/api/payment")
                || path(r).startsWith("/api/admin")
                || ("DELETE".equalsIgnoreCase(r.getMethod()) && path(r).startsWith("/api/users")), sensitive, "SENSITIVE"));
    }

    public RateLimiter resolve(HttpServletRequest request) {
        for (RouteRule rule : rules) {
            if (rule.predicate.test(request)) {
                log.debug("Route {} → {}", path(request), rule.label);
                return rule.limiter;
            }
        }
        log.debug("Route {} → GENERAL", path(request));
        return generalLimiter;
    }

    private static String path(HttpServletRequest r) { return r.getRequestURI(); }

    private record RouteRule(Predicate<HttpServletRequest> predicate, RateLimiter limiter, String label) {}
}
