package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.core.notification.domain.model.NotificationAudienceType;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import java.time.Instant;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record CreateNotificationRequest(
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
    String actionType,
    String actionUrl,
    Instant expiresAt,
    Set<NotificationChannel> channels) {}
