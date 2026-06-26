package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record NotificationIntent(
    UUID sourceEventId,
    TenantId tenantId,
    String sourceType,
    String templateKey,
    NotificationSeverity severity,
    NotificationKind kind,
    NotificationCategory category,
    NotificationAudienceType audienceType,
    Set<NotificationTarget> targets,
    Map<String, Object> variables,
    String title,
    String message,
    String correlationKey) {}
