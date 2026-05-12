package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineSalesGrantId(UUID value) {

  public OfflineSalesGrantId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineSalesGrantId.value is null");
    }
  }

  public static OfflineSalesGrantId of(UUID value) {
    return new OfflineSalesGrantId(value);
  }

  public static OfflineSalesGrantId nullableOf(UUID value) {
    return value == null ? null : new OfflineSalesGrantId(value);
  }

  public static OfflineSalesGrantId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineSalesGrantId string is required");
    }
    return new OfflineSalesGrantId(UUID.fromString(raw));
  }
}
