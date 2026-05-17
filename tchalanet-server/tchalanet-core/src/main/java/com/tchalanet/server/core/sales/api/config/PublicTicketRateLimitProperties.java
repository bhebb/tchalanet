package com.tchalanet.server.core.sales.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.public.tickets.rate-limit")
public record PublicTicketRateLimitProperties(
    boolean enabled,
    int requestsPerSecond,
    int burst
) {}
