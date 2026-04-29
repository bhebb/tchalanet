package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record NotificationDeliveryId(UUID value) {

  public NotificationDeliveryId {
    if (value == null) {
      throw new IllegalArgumentException("NotificationDeliveryId.value is null");
    }
  }

  public static NotificationDeliveryId of(UUID value) {
    return new NotificationDeliveryId(value);
  }

  public static NotificationDeliveryId nullableOf(UUID raw) {
    return raw == null ? null : new NotificationDeliveryId(raw);
  }

  public static NotificationDeliveryId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("NotificationDeliveryId string is required");
    }
    return new NotificationDeliveryId(UUID.fromString(raw));
  }
}
