package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineGrantId(UUID value) {

  public OfflineGrantId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineGrantId.value is null");
    }
  }

  public static OfflineGrantId of(UUID value) {
    return new OfflineGrantId(value);
  }

  public static OfflineGrantId nullableOf(UUID value) {
    return value == null ? null : new OfflineGrantId(value);
  }

  public static OfflineGrantId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineGrantId string is required");
    }
    return new OfflineGrantId(UUID.fromString(raw));
  }

  public UUID uuid() {
    return value;
  }
}
