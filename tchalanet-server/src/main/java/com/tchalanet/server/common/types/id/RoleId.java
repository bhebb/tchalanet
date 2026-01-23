package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Role. */
public record RoleId(UUID value) {

  public RoleId {
    if (value == null) throw new IllegalArgumentException("RoleId.value is null");
  }

  public static RoleId of(UUID value) {
    return new RoleId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static RoleId nullableOf(UUID raw) {
    return raw == null ? null : new RoleId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static RoleId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("RoleId string is required");
    }
    return new RoleId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
