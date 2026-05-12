package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record NotificationPreferenceId(UUID value) {

  public NotificationPreferenceId {
    if (value == null) {
      throw new IllegalArgumentException("NotificationPreferenceId.value is null");
    }
  }

  public static NotificationPreferenceId of(UUID value) {
    return new NotificationPreferenceId(value);
  }

  public static NotificationPreferenceId nullableOf(UUID raw) {
    return raw == null ? null : new NotificationPreferenceId(raw);
  }

  public static NotificationPreferenceId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("NotificationPreferenceId string is required");
    }
    return new NotificationPreferenceId(UUID.fromString(raw));
  }
}
