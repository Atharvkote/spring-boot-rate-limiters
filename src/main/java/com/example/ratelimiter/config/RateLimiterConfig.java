package com.example.ratelimiter.config;

import com.example.ratelimiter.config.properties.RateLimiterProperties;
import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.limiters.RateLimiter;
import com.example.ratelimiter.limiters.algos.FixedWindowAlgorithm;
import com.example.ratelimiter.limiters.algos.RateLimitAlgorithm;
import com.example.ratelimiter.limiters.algos.SlidingWindowAlgorithm;
import com.example.ratelimiter.limiters.algos.TokenBucketAlgorithm;
import com.example.ratelimiter.limiters.policies.RateLimitPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Map;

@Configuration
public class RateLimiterConfig {

    // ── Redis ───────────────────────────────────────────────

    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory cf) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(cf);
        return t;
    }

    @Bean
    public RedisScript<Long> fixedWindowScript() {
        return RedisScript.of(new ClassPathResource("scripts/fixed-window.lua"), Long.class);
    }

    @Bean
    public RedisScript<Long> slidingWindowScript() {
        return RedisScript.of(new ClassPathResource("scripts/sliding-window.lua"), Long.class);
    }

    @Bean
    public RedisScript<Long> tokenBucketScript() {
        return RedisScript.of(new ClassPathResource("scripts/token-bucket.lua"), Long.class);
    }

    // ── Algorithm map ───────────────────────────────────────

    @Bean
    public Map<AlgorithmType, RateLimitAlgorithm> algorithmMap(
            FixedWindowAlgorithm fixed,
            SlidingWindowAlgorithm sliding,
            TokenBucketAlgorithm token
    ) {
        return Map.of(
                AlgorithmType.FIXED_WINDOW, fixed,
                AlgorithmType.SLIDING_WINDOW, sliding,
                AlgorithmType.TOKEN_BUCKET, token
        );
    }

    // ── Rate Limiter beans (one per category) ───────────────

    @Bean
    public RateLimiter generalRateLimiter(RateLimiterProperties props, Map<AlgorithmType, RateLimitAlgorithm> algos) {
        return new RateLimiter("general", RateLimitPolicy.from(props.getGeneral()), algos);
    }

    @Bean
    public RateLimiter authRateLimiter(RateLimiterProperties props, Map<AlgorithmType, RateLimitAlgorithm> algos) {
        return new RateLimiter("auth", RateLimitPolicy.from(props.getAuth()), algos);
    }

    @Bean
    public RateLimiter sensitiveRateLimiter(RateLimiterProperties props, Map<AlgorithmType, RateLimitAlgorithm> algos) {
        return new RateLimiter("sensitive", RateLimitPolicy.from(props.getSensitive()), algos);
    }

    @Bean
    public RateLimiter uploadRateLimiter(RateLimiterProperties props, Map<AlgorithmType, RateLimitAlgorithm> algos) {
        return new RateLimiter("upload", RateLimitPolicy.from(props.getUpload()), algos);
    }
}
