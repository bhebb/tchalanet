package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TenantGameId(UUID value) {
  public TenantGameId {
    if (value == null) throw new IllegalArgumentException("TenantGameId.value is null");
  }

  public static TenantGameId of(UUID value) {
    return new TenantGameId(value);
  }

  /** Return TenantGameId or null if id is null */
  public static TenantGameId nullableOf(UUID id) {
    return id == null ? null : new TenantGameId(id);
  }

  public static TenantGameId of(String id) {
    if (id == null) throw new IllegalArgumentException("tenant game id string is required");
    return new TenantGameId(UUID.fromString(id));
  }

  public static TenantGameId random() {
    return new TenantGameId(UUID.randomUUID());
  }

  /** Return the underlying UUID value */
  public UUID uuid() {
    return value;
  }

  public UUID value() {
    return value;
  }
}
