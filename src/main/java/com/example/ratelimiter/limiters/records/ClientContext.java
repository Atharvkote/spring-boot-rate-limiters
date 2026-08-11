package com.example.ratelimiter.limiters.records;

import com.example.ratelimiter.enums.ClientType;

public record ClientContext(
                String clientId,
                ClientType clientType,
                String ipAddress,
                String userId,
                String httpMethod,
                String requestPath) {
}
