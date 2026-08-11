package com.example.ratelimiter.filter;

import com.example.ratelimiter.enums.FailMode;
import com.example.ratelimiter.config.properties.RateLimiterProperties;
import com.example.ratelimiter.limiters.RateLimiter;
import com.example.ratelimiter.limiters.records.ClientContext;
import com.example.ratelimiter.limiters.records.RateLimitResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterProperties properties;
    private final ClientResolver clientResolver;
    private final RateLimiterResolver limiterResolver;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RateLimitFilter(
            RateLimiterProperties properties,
            ClientResolver clientResolver,
            RateLimiterResolver limiterResolver,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.clientResolver = clientResolver;
        this.limiterResolver = limiterResolver;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result;
        try {
            ClientContext ctx = clientResolver.resolve(request);
            RateLimiter limiter = limiterResolver.resolve(request);
            result = limiter.check(ctx);
            recordMetrics(limiter.getCategory(), result);
        } catch (Exception ex) {
            log.error("Rate limiter error: {}", ex.getMessage(), ex);
            counter("rate_limit_redis_errors_total", "all").increment();

            if (properties.getFailMode() == FailMode.OPEN) {
                log.warn("Fail-OPEN: allowing request despite rate limiter error");
                filterChain.doFilter(request, response);
            } else {
                log.warn("Fail-CLOSED: rejecting request due to rate limiter error");
                write429(response, 0);
            }
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetAfterSeconds()));

        if (result.allowed()) {
            filterChain.doFilter(request, response);
        } else {
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            write429(response, result.retryAfterSeconds());
        }
    }

    private void write429(HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded",
                "retryAfterSeconds", retryAfter)));
    }

    private void recordMetrics(String category, RateLimitResult result) {
        counter("rate_limit_requests_total", category).increment();
        counter(result.allowed() ? "rate_limit_allowed_total" : "rate_limit_rejected_total", category).increment();
    }

    private Counter counter(String name, String category) {
        return Counter.builder(name).tag("category", category).register(meterRegistry);
    }
}
