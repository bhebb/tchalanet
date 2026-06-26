package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import java.time.Instant;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record Notification(
    NotificationId id,
    TenantId tenantId,
    String sourceType,
    String sourceId,
    String dedupeKey,
    NotificationAudienceType audienceType,
    Set<NotificationTarget> targets,
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
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt) {}
