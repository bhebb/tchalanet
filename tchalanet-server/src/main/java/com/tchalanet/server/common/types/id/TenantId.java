package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TenantId(UUID value) {
  public TenantId {
    if (value == null) throw new IllegalArgumentException("TenantId.value is null");
  }

  public static TenantId of(UUID value) {
    return new TenantId(value);
  }

  /** Return TenantId or null if id is null */
  public static TenantId nullableOf(UUID id) {
    return id == null ? null : new TenantId(id);
  }

  public static TenantId of(String id) {
    if (id == null) throw new IllegalArgumentException("tenant id string is required");
    return new TenantId(UUID.fromString(id));
  }

  public static TenantId random() {
    return new TenantId(UUID.randomUUID());
  }

  /** Return the underlying UUID value */
  public UUID uuid() {
    return value;
  }

  public UUID value() {
    return value;
  }
}
