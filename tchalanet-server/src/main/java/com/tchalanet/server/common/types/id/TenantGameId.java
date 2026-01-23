package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for TenantGame. */
public record TenantGameId(UUID value) {

  public TenantGameId {
    if (value == null) throw new IllegalArgumentException("TenantGameId.value is null");
  }

  public static TenantGameId of(UUID value) {
    return new TenantGameId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static TenantGameId nullableOf(UUID raw) {
    return raw == null ? null : new TenantGameId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static TenantGameId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TenantGameId string is required");
    }
    return new TenantGameId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
