package com.tchalanet.server.features.ticketdelivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.tickets.delivery")
public record TicketDeliveryProperties(
    String edgeBaseUrl,
    String edgeDeliveryPath,
    boolean enabled
) {
  public TicketDeliveryProperties {
    if (edgeBaseUrl == null || edgeBaseUrl.isBlank()) edgeBaseUrl = "http://tchalanet-edge-service:3000";
    if (edgeDeliveryPath == null || edgeDeliveryPath.isBlank()) edgeDeliveryPath = "/internal/delivery/ticket";
  }
}
