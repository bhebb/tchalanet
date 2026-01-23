package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for User. */
public record UserId(UUID value) {

  public UserId {
    if (value == null) throw new IllegalArgumentException("UserId.value is null");
  }

  public static UserId of(UUID value) {
    return new UserId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static UserId nullableOf(UUID raw) {
    return raw == null ? null : new UserId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static UserId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("UserId string is required");
    }
    return new UserId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
