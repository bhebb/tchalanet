package com.tchalanet.server.platform.notification.api.model.request;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record MarkNotificationReadRequest(@NotNull NotificationId notificationId, UserId actorId) {}
