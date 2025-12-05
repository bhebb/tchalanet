package com.tchalanet.server.core.notification.application;

import com.tchalanet.server.core.notification.domain.NotificationChannel;
import com.tchalanet.server.core.notification.domain.NotificationType;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Commande d'application pour demander l'envoi d'une notification.
 */
public record SendNotificationCommand(
    UUID tenantId,
    @Nullable UUID userId,
    String recipient,
    NotificationType type,
    NotificationChannel channel,
    String locale,
    Map<String, Object> data) {
}

