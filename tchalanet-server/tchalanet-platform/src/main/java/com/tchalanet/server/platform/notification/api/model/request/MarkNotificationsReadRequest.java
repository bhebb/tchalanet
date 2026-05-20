package com.tchalanet.server.platform.notification.api.model.request;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MarkNotificationsReadRequest(@NotEmpty List<NotificationId> notificationIds, UserId actorId) {}
