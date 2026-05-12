package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record Notification(
    NotificationId id,
    TenantId tenantId,
    String sourceType,
    String sourceId,
    String dedupeKey,
    NotificationAudienceType audienceType,
    String audienceValue,
    NotificationSeverity severity,
    NotificationKind kind,
    NotificationCategory category,
    String titleKey,
    String messageKey,
    String titleText,
    String messageText,
    JsonNode payload,
    NotificationAction action,
    NotificationStatus status,
    Instant readAt,
    Instant archivedAt,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt) {}
