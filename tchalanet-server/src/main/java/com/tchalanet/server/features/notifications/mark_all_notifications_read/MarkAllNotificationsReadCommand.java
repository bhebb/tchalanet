package com.tchalanet.server.features.notifications.mark_all_notifications_read;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record MarkAllNotificationsReadCommand(TenantId tenantId, UserId userId, Instant readAt) {}
