package com.tchalanet.server.core.notification.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record ScheduleNotificationDeliveryCommand(
    @NotNull NotificationId notificationId,
    @NotNull NotificationChannel channel,
    @NotBlank String recipient,
    Instant nextAttemptAt,
    JsonNode payload)
    implements Command<NotificationDeliveryId> {}
