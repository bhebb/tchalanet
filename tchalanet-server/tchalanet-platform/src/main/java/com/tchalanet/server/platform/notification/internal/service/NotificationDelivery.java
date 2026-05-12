package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record NotificationDelivery(
    NotificationDeliveryId id,
    TenantId tenantId,
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
    JsonNode payload,
    Instant createdAt,
    Instant updatedAt) {}
