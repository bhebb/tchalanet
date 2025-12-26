package com.tchalanet.server.features.notifications.mark_all_notifications_read;

import java.time.Instant;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record MarkAllNotificationsReadCommand(TenantId tenantId, UserId userId, Instant readAt) {
}
