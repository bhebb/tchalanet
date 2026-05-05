package com.tchalanet.server.common.notification.model;

import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.common.types.enums.NotificationType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Payload générique décrivant une notification à envoyer via le gateway.
 * Le target peut être null pour les notifications techniques/batch.
 */
public record SendNotificationPayload(
    NotificationType type,
    NotificationChannel channel,
    NotificationTarget target,
    Locale locale,
    Map<String, Object> data) {

    public SendNotificationPayload {
        Objects.requireNonNull(type);
        Objects.requireNonNull(channel);
        // target can be null for technical/batch notifications
        Objects.requireNonNull(data);
    }
}
