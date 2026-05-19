package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineCodeId(UUID value) {

  public OfflineCodeId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineCodeId.value is null");
    }
  }

  public static OfflineCodeId of(UUID value) {
    return new OfflineCodeId(value);
  }

  public static OfflineCodeId nullableOf(UUID value) {
    return value == null ? null : new OfflineCodeId(value);
  }

  public static OfflineCodeId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineCodeId string is required");
    }
    return new OfflineCodeId(UUID.fromString(raw));
  }

  public UUID uuid() {
    return value;
  }
}
