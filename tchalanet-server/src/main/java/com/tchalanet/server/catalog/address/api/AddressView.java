package com.tchalanet.server.catalog.address.api;

import java.time.Instant;

import com.tchalanet.server.common.types.id.AddressId;

/** Read-only view for an Address in catalog.address */
public record AddressView(
    AddressId id,
    String line1,
    String line2,
    String city,
    String postalCode,
    String country,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {
}
