package com.tchalanet.server.core.notification.application.query.model;

import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import java.time.Instant;

public record NotificationDeliveryView(
    NotificationDeliveryId id,
    NotificationId notificationId,
    NotificationChannel channel,
    String recipient,
    NotificationDeliveryStatus status,
    int attemptCount,
    Instant nextAttemptAt,
    Instant lastAttemptAt,
    String provider,
    String providerMessageId,
    String errorCode,
    String errorMessage,
    Instant createdAt) {}
