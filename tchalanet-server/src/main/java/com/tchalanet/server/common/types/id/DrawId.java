package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Draw. */
public record DrawId(UUID value) {

  public DrawId {
    if (value == null) throw new IllegalArgumentException("DrawId.value is null");
  }

  public static DrawId of(UUID value) {
    return new DrawId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static DrawId nullableOf(UUID raw) {
    return raw == null ? null : new DrawId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static DrawId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("DrawId string is required");
    }
    return new DrawId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
