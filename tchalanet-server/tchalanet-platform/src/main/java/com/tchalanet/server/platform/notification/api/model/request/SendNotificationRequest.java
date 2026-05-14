package com.tchalanet.server.platform.notification.api.model.request;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;

import com.tchalanet.server.platform.notification.api.model.NotificationType;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Commande d'application pour demander l'envoi d'une notification.
 * Supporte plusieurs destinataires et canaux différents.
 */
public record SendNotificationRequest(
    NotificationType type,
    NotificationSeverity severity,
    List<NotificationRecipient> recipients,
    Locale locale,
    String title,
    String message,
    Map<String, Object> context,
    @Nullable String idempotencyKey,
    @Nullable String reason
) {
    public SendNotificationRequest {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity is required");
        }
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("at least one recipient is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
