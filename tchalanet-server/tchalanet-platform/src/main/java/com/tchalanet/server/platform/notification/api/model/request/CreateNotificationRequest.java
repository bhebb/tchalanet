package com.tchalanet.server.platform.notification.api.model.request;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record CreateNotificationRequest(
    TenantId tenantId,
    String sourceType,
    String sourceId,
    String dedupeKey,
    @NotNull NotificationAudienceType audienceType,
    Set<NotificationTarget> targets,
    @NotNull NotificationSeverity severity,
    @NotNull NotificationKind kind,
    @NotNull NotificationCategory category,
    String titleKey,
    String messageKey,
    String titleText,
    String messageText,
    Map<String, NotificationTranslationInput> translations,
    JsonNode payload,
    String actionType,
    String actionUrl,
    Instant expiresAt,
    Set<NotificationChannel> channels) {}
