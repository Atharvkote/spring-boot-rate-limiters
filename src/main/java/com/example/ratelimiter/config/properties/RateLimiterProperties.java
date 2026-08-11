package com.example.ratelimiter.config.properties;

import com.example.ratelimiter.enums.AlgorithmType;
import com.example.ratelimiter.enums.ClientType;
import com.example.ratelimiter.enums.FailMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private boolean enabled = true;
    private FailMode failMode = FailMode.OPEN;
    private PolicyConfig general = new PolicyConfig();
    private PolicyConfig auth = new PolicyConfig();
    private PolicyConfig sensitive = new PolicyConfig();
    private PolicyConfig upload = new PolicyConfig();

    @Data
    public static class PolicyConfig {
        private int limit;
        private Duration window = Duration.ofMinutes(1);
        private AlgorithmType algorithm = AlgorithmType.FIXED_WINDOW;
        private ClientType clientType = ClientType.IP;
    }
}
