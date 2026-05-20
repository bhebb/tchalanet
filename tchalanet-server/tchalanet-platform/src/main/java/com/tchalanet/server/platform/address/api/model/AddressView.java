package com.tchalanet.server.platform.address.api.model;

import com.tchalanet.server.common.types.id.AddressId;

/**
 * View DTO for address read operations.
 * Immutable projection of an address for API responses.
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
