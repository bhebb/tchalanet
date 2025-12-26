package com.tchalanet.server.core.external.infra.http;

import com.tchalanet.server.core.external.infra.config.NotificationServiceProperties;
import com.tchalanet.server.core.notification.domain.SendNotificationPayload;
import com.tchalanet.server.core.notification.port.NotificationGatewayPort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Adapter HTTP vers le serveur Node de notifications, implémentant le port du core. */
@Component
public class NotificationHttpGatewayAdapter implements NotificationGatewayPort {

  private final WebClient webClient;
  private final NotificationServiceProperties properties;

  public NotificationHttpGatewayAdapter(
      WebClient webClient, NotificationServiceProperties properties) {
    this.webClient = webClient;
    this.properties = properties;
  }

  @Override
  public void send(SendNotificationPayload payload) {
    // TODO: effectuer un POST vers {baseUrl}/api/notifications avec le payload sérialisé.
    // Exemple:
    // String url = properties.getBaseUrl() + "/api/notifications";
    // restTemplate.postForLocation(url, payload);
  }
}
