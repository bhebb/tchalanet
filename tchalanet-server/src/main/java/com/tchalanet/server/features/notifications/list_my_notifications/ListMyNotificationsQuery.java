package com.tchalanet.server.features.notifications.list_my_notifications;

import java.util.UUID;

public record ListMyNotificationsQuery(UUID tenantId, UUID userId, boolean unreadOnly, int page, int size) {
}
