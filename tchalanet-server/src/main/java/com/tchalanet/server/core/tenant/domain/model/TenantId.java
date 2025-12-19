package com.tchalanet.server.core.tenant.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value object identifier for Tenant. */
public record TenantId(UUID id) {

  public TenantId {
    Objects.requireNonNull(id, "tenant id is required");
  }

  /**
   * Static factory from UUID.
   */
  public static TenantId of(UUID id) {
    return new TenantId(Objects.requireNonNull(id, "tenant id is required"));
  }

  /**
   * Static factory from String representation of UUID.
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static TenantId of(String id) {
    Objects.requireNonNull(id, "tenant id string is required");
    return new TenantId(UUID.fromString(id));
  }

  public UUID value() {
    return id;
  }

  @Override
  public String toString() {
    return id.toString();
  }
}
