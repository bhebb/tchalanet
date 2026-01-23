package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Tenant. */
public record TenantId(UUID value) {

  public TenantId {
    if (value == null) throw new IllegalArgumentException("TenantId.value is null");
  }

  public static TenantId of(UUID value) {
    return new TenantId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static TenantId nullableOf(UUID raw) {
    return raw == null ? null : new TenantId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static TenantId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TenantId string is required");
    }
    return new TenantId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
