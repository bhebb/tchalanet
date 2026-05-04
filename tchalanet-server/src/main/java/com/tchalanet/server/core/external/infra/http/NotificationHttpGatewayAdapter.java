package com.tchalanet.server.core.external.infra.http;

import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.core.notification.infra.config.NodeNotificationConfigProperties;
import com.tchalanet.server.common.notification.NotificationGatewayPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Adapter HTTP vers le serveur Node de notifications, implémentant le port du core. */
@Component
@Primary
public class NotificationHttpGatewayAdapter implements NotificationGatewayPort {

  private final WebClient webClient;
  private final NodeNotificationConfigProperties properties;

  public NotificationHttpGatewayAdapter(
      WebClient webClient, NodeNotificationConfigProperties properties) {
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
