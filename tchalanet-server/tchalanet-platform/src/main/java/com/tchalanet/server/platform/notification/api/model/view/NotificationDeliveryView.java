package com.tchalanet.server.platform.notification.api.model.view;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;

import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
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
