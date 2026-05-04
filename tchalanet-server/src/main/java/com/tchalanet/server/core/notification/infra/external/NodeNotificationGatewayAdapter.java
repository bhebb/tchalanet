package com.tchalanet.server.core.notification.infra.external;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.core.notification.infra.config.NodeNotificationConfigProperties;
import com.tchalanet.server.common.notification.NotificationGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NodeNotificationGatewayAdapter implements NotificationGatewayPort {
    private final NodeNotificationConfigProperties properties;
    private final RestClient nodeNotificationClient;

    @Override
    public void send(SendNotificationPayload payload) {
        log.info(
            "Sending notification: tenantId={} type={} channel={}",
            payload.target().tenantId(),
            payload.type(),
            payload.channel());
        if (!properties.enabled()) {
            return;
        }

        nodeNotificationClient.post()
            .uri(properties.basePath())
            .body(toRequest(payload))
            .retrieve()
            .toBodilessEntity();
    }

    private NodeNotificationRequest toRequest(SendNotificationPayload payload) {
        var tenantId = payload.target().tenantId() != null
            ? payload.target().tenantId()
            : null;

        var userId = payload.target().userId() != null
            ? payload.target().userId()
            : null;
        return new NodeNotificationRequest(
            payload.type().name(),
            payload.channel().name(),
            tenantId,
            userId,
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
        Locale locale,
        Map<String, Object> data) {
    }
}
