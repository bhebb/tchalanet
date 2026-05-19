package com.tchalanet.server.core.limitpolicy.internal.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Default offline policy used by {@code GetOfflineLimitPolicyQueryHandler} when no
 * per-tenant override is configured.
 *
 * <p>TODO Phase G/H: back this with a persisted per-tenant policy table; the static
 * defaults below cover dev + first-launch tenants.
 */
@ConfigurationProperties(prefix = "tch.limitpolicy.offline")
public record OfflineLimitPolicyProperties(
    boolean enabled,
    int batchSize,
    Duration validityDuration,
    Duration syncAcceptedExtension,
    int maxTicketCount,
    BigDecimal maxTotalAmount,
    String currency
) {

    public static OfflineLimitPolicyProperties defaults() {
        return new OfflineLimitPolicyProperties(
            true,
            50,
            Duration.ofHours(8),
            Duration.ofHours(72),
            50,
            new BigDecimal("100000.00"),
            "HTG"
        );
    }

    public OfflineLimitPolicyProperties withDefaults() {
        var d = defaults();
        return new OfflineLimitPolicyProperties(
            enabled,
            batchSize > 0 ? batchSize : d.batchSize(),
            validityDuration != null ? validityDuration : d.validityDuration(),
            syncAcceptedExtension != null ? syncAcceptedExtension : d.syncAcceptedExtension(),
            maxTicketCount > 0 ? maxTicketCount : d.maxTicketCount(),
            maxTotalAmount != null ? maxTotalAmount : d.maxTotalAmount(),
            currency != null && !currency.isBlank() ? currency : d.currency()
        );
    }
}
