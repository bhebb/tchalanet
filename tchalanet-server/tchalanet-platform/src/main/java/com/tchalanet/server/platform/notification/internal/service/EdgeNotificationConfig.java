package com.tchalanet.server.platform.notification.internal.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour le module notification (flows).
 */
@Configuration
@EnableConfigurationProperties(NotificationFlowProperties.class)
class NotificationConfig {
}
