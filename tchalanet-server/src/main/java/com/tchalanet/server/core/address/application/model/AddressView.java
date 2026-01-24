package com.tchalanet.server.core.address.application.model;

import com.tchalanet.server.common.types.id.AddressId;

/**
 * View DTO for address read operations.
 * Immutable projection of Address for API responses.
 * Per typed_ids.md: uses AddressId wrapper (not raw UUID).
 */
public record AddressView(
    AddressId id,
    String line1,
    String line2,
    String city,
    String region,
    String country,
    String postalCode
) {}
