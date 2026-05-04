package com.tchalanet.server.core.notification.infra.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.notification.node")
public record NodeNotificationConfigProperties(String baseUrl, String basePath, Duration timeout, boolean enabled) {}
