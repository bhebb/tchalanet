package com.tchalanet.server.platform.notification.api.model.request;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;

import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record ScheduleNotificationDeliveryRequest(
    @NotNull NotificationId notificationId,
    @NotNull NotificationChannel channel,
    @NotBlank String recipient,
    Instant nextAttemptAt,
    JsonNode payload) {}
