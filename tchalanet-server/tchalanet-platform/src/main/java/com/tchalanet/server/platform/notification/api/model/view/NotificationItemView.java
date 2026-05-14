package com.tchalanet.server.platform.notification.api.model.view;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;

import com.tchalanet.server.common.types.id.NotificationId;
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
