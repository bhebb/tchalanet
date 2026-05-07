package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour le module notification (flows).
 */
@Configuration
@EnableConfigurationProperties(NotificationFlowProperties.class)
class NotificationConfig {
}
