package com.tchalanet.server.core.sales.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// TODO: per-tenant configuration via platform.tenantconfig (V1.5)
@ConfigurationProperties(prefix = "tch.sales.tickets.visibility")
public record TicketVisibilityProperties(int publicVisibilityDays) {
    public TicketVisibilityProperties {
        if (publicVisibilityDays <= 0) publicVisibilityDays = 90;
    }
}
