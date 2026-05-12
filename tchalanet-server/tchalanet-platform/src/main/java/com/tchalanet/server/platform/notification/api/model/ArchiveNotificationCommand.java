package com.tchalanet.server.platform.notification.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record ArchiveNotificationCommand(@NotNull NotificationId notificationId, UserId actorId)
    implements Command<Void> {}
