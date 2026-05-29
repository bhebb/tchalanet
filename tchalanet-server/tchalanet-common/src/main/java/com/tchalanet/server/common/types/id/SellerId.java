package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SellerId(UUID value) {
  public SellerId {
    if (value == null) {
      throw new IllegalArgumentException("SellerId.value is null");
    }
  }

  public static SellerId of(UUID value) { return new SellerId(value); }
  public static SellerId nullableOf(UUID raw) { return raw == null ? null : new SellerId(raw); }
  public static SellerId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("SellerId string is required");
    return new SellerId(UUID.fromString(raw));
  }
}
