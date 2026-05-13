package com.tchalanet.server.platform.communication.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Edge-service communication integration properties. */
@ConfigurationProperties(prefix = "tch.communication.edge")
public record EdgeCommunicationProperties(
    boolean enabled,
    String baseUrl,
    String messagesPath,
    String hmacSecret,
    Duration connectTimeout,
    Duration readTimeout
) {
    public EdgeCommunicationProperties {
        if (enabled && baseUrl == null) {
            throw new IllegalArgumentException("baseUrl is required when edge communication is enabled");
        }
        if (enabled && messagesPath == null) {
            throw new IllegalArgumentException("messagesPath is required when edge communication is enabled");
        }
        if (enabled && hmacSecret == null) {
            throw new IllegalArgumentException("hmacSecret is required when edge communication is enabled");
        }
    }
}
