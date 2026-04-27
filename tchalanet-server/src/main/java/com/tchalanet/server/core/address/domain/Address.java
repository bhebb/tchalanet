package com.tchalanet.server.core.address.domain;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for Address (tenant-scoped).
 * Represents a physical/postal address with deduplication support.
 * Provides factory methods for construction (createNew, restore).
 */
public record Address(
    AddressId id,
    TenantId tenantId,
    String line1,
    String line2,
    String city,
    String region,
    String country,
    String postalCode,
    String normalizedKey,
    boolean deleted,
    Instant createdAt,
    Instant updatedAt
) {

  public Address {
    if (id == null) {
      throw new IllegalArgumentException("Address.id is required");
    }
    if (tenantId == null) {
      throw new IllegalArgumentException("Address.tenantId is required");
    }
    if (line1 == null || line1.isBlank()) {
      throw new IllegalArgumentException("Address.line1 is required");
    }
    if (normalizedKey == null || normalizedKey.isBlank()) {
      throw new IllegalArgumentException("Address.normalizedKey is required");
    }
  }

  /**
   * Factory: create a new active address.
   * Generated ID, createdAt = updatedAt = now.
   */
  public static Address createNew(
      TenantId tenantId,
      String line1,
      String line2,
      String city,
      String region,
      String country,
      String postalCode,
      String normalizedKey,
      Instant now) {
    return new Address(
        AddressId.of(UUID.randomUUID()),
        tenantId,
        line1,
        line2,
        city,
        region,
        country,
        postalCode,
        normalizedKey,
        false, // deleted=false (new is active)
        now,
        now);
  }

  /**
   * Factory: restore/update an existing address.
   * Preserves ID, tenantId. createdAt typically null (impl should preserve existing).
   */
  public static Address restore(
      AddressId id,
      TenantId tenantId,
      String line1,
      String line2,
      String city,
      String region,
      String country,
      String postalCode,
      String normalizedKey,
      boolean deleted,
      Instant createdAt,
      Instant updatedAt) {
    return new Address(
        id,
        tenantId,
        line1,
        line2,
        city,
        region,
        country,
        postalCode,
        normalizedKey,
        deleted,
        createdAt,
        updatedAt);
  }
}
