package com.tchalanet.server.platform.notification.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.notification.domain.model.NotificationAudienceType;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record CreateNotificationCommand(
    TenantId tenantId,
    String sourceType,
    String sourceId,
    String dedupeKey,
    @NotNull NotificationAudienceType audienceType,
    @NotBlank String audienceValue,
    @NotNull NotificationSeverity severity,
    @NotNull NotificationKind kind,
    @NotNull NotificationCategory category,
    String titleKey,
    String messageKey,
    String titleText,
    String messageText,
    JsonNode payload,
    String actionType,
    String actionUrl,
    Instant expiresAt,
    Set<NotificationChannel> channels)
    implements Command<Void> {}
