package com.tchalanet.server.platform.notification.api.model;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record NotificationPublishedEvent(
    UUID eventId,
    NotificationId notificationId,
    NotificationPublicationId publicationId,
    TenantId tenantId,
    NotificationAudienceType audienceType,
    Set<NotificationTarget> targets,
    NotificationSeverity severity,
    NotificationKind kind,
    NotificationCategory category,
    String title,
    String message,
    JsonNode payload,
    String actionUrl,
    Set<NotificationDeliveryChannel> deliveryChannels,
    Instant occurredAt) {}
