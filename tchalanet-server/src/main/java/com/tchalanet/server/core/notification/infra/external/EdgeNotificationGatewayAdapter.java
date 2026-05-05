package com.tchalanet.server.core.notification.infra.external;

import com.tchalanet.server.common.notification.NotificationGatewayPort;
import com.tchalanet.server.common.notification.model.NotificationTarget;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.core.notification.infra.config.EdgeNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptateur pour envoyer des notifications via tchalanet-edge-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EdgeNotificationGatewayAdapter implements NotificationGatewayPort {

    private final EdgeNotificationProperties properties;
    private final EdgeHmacSigner hmacSigner;
    private final RestClient edgeNotificationClient;

    @Override
    public void send(SendNotificationPayload payload) {
        if (!properties.enabled()) {
            log.debug("Edge notification integration disabled, skipping send");
            return;
        }

        log.info(
            "Sending edge notification: type={} channel={} target={}",
            payload.type(),
            payload.channel(),
            payload.target() != null ? payload.target().recipient() : "N/A"
        );

        var request = toEdgeRequest(payload);
        var signed = hmacSigner.sign(properties.hmacSecret(), request);

        try {
            edgeNotificationClient.post()
                .uri(properties.notificationsPath())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", extractRequestId(payload))
                .header("Idempotency-Key", generateIdempotencyKey(payload))
                .header("X-Tch-Timestamp", signed.timestamp())
                .header("X-Tch-Signature", signed.signature())
                .body(signed.rawJsonBody())
                .retrieve()
                .toBodilessEntity();

            log.debug("Edge notification sent successfully");

        } catch (Exception e) {
            log.error("Failed to send edge notification: type={} channel={}",
                payload.type(), payload.channel(), e);
            throw new EdgeNotificationException("Failed to send notification through edge service", e);
        }
    }

    private EdgeNotificationRequest toEdgeRequest(SendNotificationPayload payload) {
        var eventId = extractEventId(payload);
        var severity = extractSeverity(payload);
        var title = extractTitle(payload);
        var message = extractMessage(payload);
        var recipients = buildRecipients(payload);
        var context = buildContext(payload);

        return new EdgeNotificationRequest(eventId, severity, title, message, recipients, context);
    }

    private List<EdgeNotificationRecipient> buildRecipients(SendNotificationPayload payload) {
        var channel = payload.channel().name();
        var target = payload.target();

        return switch (payload.channel()) {
            case SLACK -> {
                var channelKey = extractSlackChannelKey(payload);
                yield List.of(new EdgeNotificationRecipient(channel, null, channelKey));
            }
            case EMAIL, SMS -> {
                var to = target != null ? target.recipient() : null;
                yield List.of(new EdgeNotificationRecipient(channel, to, null));
            }
            default ->
                throw new IllegalArgumentException("Unsupported notification channel: " + payload.channel());
        };
    }

    private Map<String, Object> buildContext(SendNotificationPayload payload) {
        var context = new HashMap<String, Object>();

        if (payload.data() != null) {
            context.putAll(payload.data());
        }

        // Convertir les typed IDs en strings pour edge
        var target = payload.target();
        if (target != null) {
            if (target.tenantId() != null) {
                context.put("tenantId", target.tenantId().value().toString());
            }
            if (target.userId() != null) {
                context.put("userId", target.userId().value().toString());
            }
        }

        if (payload.locale() != null) {
            context.put("locale", payload.locale().toString());
        }

        context.put("type", payload.type().name());

        return context;
    }

    private String extractEventId(SendNotificationPayload payload) {
        if (payload.data() != null && payload.data().containsKey("eventId")) {
            return payload.data().get("eventId").toString();
        }
        return "evt_" + UUID.randomUUID();
    }

    private String extractSeverity(SendNotificationPayload payload) {
        if (payload.data() != null && payload.data().containsKey("severity")) {
            return payload.data().get("severity").toString();
        }
        return "INFO";
    }

    private String extractTitle(SendNotificationPayload payload) {
        if (payload.data() != null && payload.data().containsKey("title")) {
            return payload.data().get("title").toString();
        }
        return payload.type().name();
    }

    private String extractMessage(SendNotificationPayload payload) {
        if (payload.data() != null && payload.data().containsKey("message")) {
            return payload.data().get("message").toString();
        }
        return "";
    }

    private String extractSlackChannelKey(SendNotificationPayload payload) {
        if (payload.data() != null && payload.data().containsKey("channelKey")) {
            return payload.data().get("channelKey").toString();
        }
        return "batch-draws"; // default for batch notifications
    }

    private String extractRequestId(SendNotificationPayload payload) {
        if (payload.data() != null && payload.data().containsKey("requestId") && payload.data().get("requestId")  != null) {
            return payload.data().get("requestId").toString();
        }
        return UUID.randomUUID().toString();
    }

    private String generateIdempotencyKey(SendNotificationPayload payload) {
        var requestId = extractRequestId(payload);
        var type = payload.type().name();
        return type + "_" + requestId;
    }

    /**
     * Exception levée lorsque l'appel vers edge-service échoue.
     */
    public static class EdgeNotificationException extends RuntimeException {
        public EdgeNotificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * DTO pour la requête edge-service.
     */
    record EdgeNotificationRequest(
        String eventId,
        String severity,
        String title,
        String message,
        List<EdgeNotificationRecipient> recipients,
        Map<String, Object> context
    ) {}

    /**
     * DTO pour un destinataire edge-service.
     */
    record EdgeNotificationRecipient(
        String channel,
        String to,
        String channelKey
    ) {}
}

