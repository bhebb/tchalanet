package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propriétés de configuration pour l'intégration avec tchalanet-edge-service.
 */
@ConfigurationProperties(prefix = "tch.notification.edge")
public record EdgeNotificationProperties(
    boolean enabled,
    String baseUrl,
    String notificationsPath,
    String hmacSecret,
    Duration connectTimeout,
    Duration readTimeout
) {
    public EdgeNotificationProperties {
        if (enabled && baseUrl == null) {
            throw new IllegalArgumentException("baseUrl is required when edge notification is enabled");
        }
        if (enabled && notificationsPath == null) {
            throw new IllegalArgumentException("notificationsPath is required when edge notification is enabled");
        }
        if (enabled && hmacSecret == null) {
            throw new IllegalArgumentException("hmacSecret is required when edge notification is enabled");
        }
    }
}

