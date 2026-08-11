package com.example.ratelimiter.core;

import com.example.ratelimiter.config.ClientType;

public record ClientContext(
    String clientId,
    ClientType clientType,
    String ipAddress,
    String userId,
    String httpMethod,
    String requestPath
) {}
