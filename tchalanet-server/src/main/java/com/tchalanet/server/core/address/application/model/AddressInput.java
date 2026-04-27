package com.tchalanet.server.core.address.application.model;

/**
 * Input DTO for address operations.
 * Used by AddressCrudService and other services that need to create/update addresses.
 */
public record AddressInput(
    String line1,
    String line2,
    String city,
    String region,
    String country,
    String postalCode
) {

  public AddressInput {
    if (line1 == null || line1.isBlank()) {
      throw new IllegalArgumentException("line1 is required");
    }
    if (city == null || city.isBlank()) {
      throw new IllegalArgumentException("city is required");
    }
    if (country == null || country.isBlank()) {
      throw new IllegalArgumentException("country is required");
    }
  }
}
