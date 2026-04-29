package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record NotificationId(UUID value) {

  public NotificationId {
    if (value == null) {
      throw new IllegalArgumentException("NotificationId.value is null");
    }
  }

  public static NotificationId of(UUID value) {
    return new NotificationId(value);
  }

  public static NotificationId nullableOf(UUID raw) {
    return raw == null ? null : new NotificationId(raw);
  }

  public static NotificationId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("NotificationId string is required");
    }
    return new NotificationId(UUID.fromString(raw));
  }
}
