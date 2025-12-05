package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tch.notification.node")
public record NodeNotificationConfigProperties(
    String baseUrl,
    String basePath,
    Duration timeout
) {
}
