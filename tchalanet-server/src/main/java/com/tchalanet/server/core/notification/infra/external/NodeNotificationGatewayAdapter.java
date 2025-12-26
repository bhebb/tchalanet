package com.tchalanet.server.core.notification.infra.external;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.notification.domain.SendNotificationPayload;
import com.tchalanet.server.core.notification.infra.config.NodeNotificationConfigProperties;
import com.tchalanet.server.core.notification.port.NotificationGatewayPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class NodeNotificationGatewayAdapter implements NotificationGatewayPort {
  private final WebClient nodeNotificationClient;
  private final NodeNotificationConfigProperties properties;

  @Override
  public void send(SendNotificationPayload payload) {
    log.info(
        "Sending notification: tenantId={} type={} channel={}",
        payload.target().tenantId(),
        payload.type(),
        payload.channel());

    nodeNotificationClient
        .post()
        .uri(properties.basePath()) // ex: /api/notifications
        .bodyValue(toRequest(payload))
        .retrieve()
        .toBodilessEntity()
        .block(properties.timeout());

    // TODO : gérer erreurs de façon plus fine (retry, mapping d’exception)
  }

  private NodeNotificationRequest toRequest(SendNotificationPayload payload) {
    var tenantId = payload.target().tenantId() != null ? payload.target().tenantId().uuid() : null;
    var userId = payload.target().userId() != null ? payload.target().userId().uuid() : null;

    return new NodeNotificationRequest(
        payload.type().name(),
        payload.channel().name(),
        TenantId.of(tenantId),
        UserId.of(userId),
        payload.target().recipient(),
        payload.locale(),
        payload.data());
  }

  private record NodeNotificationRequest(
      String type,
      String channel,
      TenantId tenantId,
      UserId userId,
      String recipient,
      String locale,
      Map<String, Object> data) {}
}
