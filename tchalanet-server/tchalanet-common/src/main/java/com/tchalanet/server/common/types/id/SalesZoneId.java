package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SalesZoneId(UUID value) {
  public SalesZoneId {
    if (value == null) {
      throw new IllegalArgumentException("SalesZoneId.value is null");
    }
  }

  public static SalesZoneId of(UUID value) { return new SalesZoneId(value); }
  public static SalesZoneId nullableOf(UUID raw) { return raw == null ? null : new SalesZoneId(raw); }
  public static SalesZoneId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("SalesZoneId string is required");
    return new SalesZoneId(UUID.fromString(raw));
  }
}
