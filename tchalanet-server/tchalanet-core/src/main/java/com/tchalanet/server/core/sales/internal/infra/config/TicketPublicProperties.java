package com.tchalanet.server.core.sales.internal.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.tickets.public")
public record TicketPublicProperties(
    String baseUrl,
    String ticketPathTemplate
) {
  public TicketPublicProperties {
    if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://app.tchalanet.com";
    if (ticketPathTemplate == null || ticketPathTemplate.isBlank()) ticketPathTemplate = "/ticket/{code}";
  }
}
