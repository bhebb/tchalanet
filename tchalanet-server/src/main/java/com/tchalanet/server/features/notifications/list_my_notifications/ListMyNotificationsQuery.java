package com.tchalanet.server.features.notifications.list_my_notifications;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record ListMyNotificationsQuery(
    TenantId tenantId, UserId userId, boolean unreadOnly, int page, int size) {}
