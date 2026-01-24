package com.tchalanet.server.core.address.domain;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/**
 * Domain model for Address (tenant-scoped).
 * Represents a physical/postal address with deduplication support.
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
}
