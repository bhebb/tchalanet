package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for DrawChannel. */
public record DrawChannelId(UUID value) {

  public DrawChannelId {
    if (value == null) throw new IllegalArgumentException("DrawChannelId.value is null");
  }

  public static DrawChannelId of(UUID value) {
    return new DrawChannelId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static DrawChannelId nullableOf(UUID raw) {
    return raw == null ? null : new DrawChannelId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static DrawChannelId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("DrawChannelId string is required");
    }
    return new DrawChannelId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
