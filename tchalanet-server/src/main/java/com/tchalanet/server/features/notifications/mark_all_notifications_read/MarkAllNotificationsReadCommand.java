package com.tchalanet.server.features.notifications.mark_all_notifications_read;

import java.time.Instant;
import java.util.UUID;

public record MarkAllNotificationsReadCommand(UUID tenantId, UUID userId, Instant readAt) {
}
