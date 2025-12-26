package com.tchalanet.server.core.notification.domain;

import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.common.types.enums.NotificationType;
import java.util.Map;
import java.util.Objects;

/** Payload générique décrivant une notification à envoyer via le gateway. */
public record SendNotificationPayload(
    NotificationType type,
    NotificationChannel channel,
    NotificationTarget target,
    String locale,
    Map<String, Object> data) {

  public SendNotificationPayload {
    Objects.requireNonNull(type);
    Objects.requireNonNull(channel);
    Objects.requireNonNull(target);
    Objects.requireNonNull(locale);
    Objects.requireNonNull(data);
  }
}
