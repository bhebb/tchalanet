package com.tchalanet.server.core.external.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du service externe de notifications (serveur Node).
 */
@ConfigurationProperties(prefix = "tch.notification")
public class NotificationServiceProperties {

  /** Base URL du service Node, ex: "http://notification-node:3000". */
  private String baseUrl;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}

