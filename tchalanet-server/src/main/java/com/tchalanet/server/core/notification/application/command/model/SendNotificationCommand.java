package com.tchalanet.server.core.notification.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.NotificationType;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Commande d'application pour demander l'envoi d'une notification.
 * Supporte plusieurs destinataires et canaux différents.
 */
public record SendNotificationCommand(
    NotificationType type,
    NotificationSeverity severity,
    List<NotificationRecipient> recipients,
    Locale locale,
    String title,
    String message,
    Map<String, Object> context,
    @Nullable String idempotencyKey,
    @Nullable String reason
) implements Command<SendNotificationResult> {
    public SendNotificationCommand {
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
