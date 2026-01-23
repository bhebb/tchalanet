package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Session. */
public record SessionId(UUID value) {

  public SessionId {
    if (value == null) throw new IllegalArgumentException("SessionId.value is null");
  }

  public static SessionId of(UUID value) {
    return new SessionId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static SessionId nullableOf(UUID raw) {
    return raw == null ? null : new SessionId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static SessionId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("SessionId string is required");
    }
    return new SessionId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
