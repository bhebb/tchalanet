package com.tchalanet.server.features.notifications.shared;

import com.tchalanet.server.common.types.enums.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

/** DTO de notification partagé entre les différents use cases côté UI. */
public record NotificationDto(
    UUID id,
    UUID userId,
    UUID tenantId,
    NotificationChannel channel,
    NotificationType type,
    NotificationDisplayType displayType,
    String title,
    String body,
    boolean read,
    Instant createdAt,
    Instant readAt) {}
