package com.tchalanet.server.core.notification.application.query.model;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record NotificationItemView(
    NotificationId id,
    NotificationSeverity severity,
    NotificationKind kind,
    NotificationCategory category,
    String titleKey,
    String messageKey,
    String titleText,
    String messageText,
    JsonNode payload,
    NotificationActionView action,
    NotificationStatus status,
    Instant readAt,
    Instant archivedAt,
    Instant expiresAt,
    Instant createdAt) {}
