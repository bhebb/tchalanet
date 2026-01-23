package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Outlet. */
public record OutletId(UUID value) {

  public OutletId {
    if (value == null) throw new IllegalArgumentException("OutletId.value is null");
  }

  public static OutletId of(UUID value) {
    return new OutletId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static OutletId nullableOf(UUID raw) {
    return raw == null ? null : new OutletId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static OutletId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OutletId string is required");
    }
    return new OutletId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
