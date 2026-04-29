package com.tchalanet.server.core.notification.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MarkNotificationsReadCommand(@NotEmpty List<NotificationId> notificationIds, UserId actorId)
    implements Command<Void> {}
