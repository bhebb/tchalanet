package com.tchalanet.server.platform.address.api.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.platform.address.internal.service.Address;

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
) {

  /**
   * Factory: convert domain Address to view.
   */
  public static AddressView fromDomain(Address address) {
    return new AddressView(
        address.id(),
        address.line1(),
        address.line2(),
        address.city(),
        address.region(),
        address.country(),
        address.postalCode()
    );
  }
}
