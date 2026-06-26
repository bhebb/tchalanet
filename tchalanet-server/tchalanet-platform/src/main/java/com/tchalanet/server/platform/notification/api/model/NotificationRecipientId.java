package com.tchalanet.server.platform.notification.api.model;

import java.util.UUID;

public record NotificationRecipientId(UUID value) {

  public NotificationRecipientId {
    if (value == null) {
      throw new IllegalArgumentException("notification recipient id is required");
    }
  }

  public static NotificationRecipientId of(UUID value) {
    return new NotificationRecipientId(value);
  }

  public static NotificationRecipientId nullableOf(UUID value) {
    return value == null ? null : new NotificationRecipientId(value);
  }
}
