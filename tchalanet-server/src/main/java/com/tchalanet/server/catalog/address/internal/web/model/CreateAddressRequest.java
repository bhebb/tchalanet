package com.tchalanet.server.catalog.address.internal.web.model;

public record CreateAddressRequest(
    String line1,
    String line2,
    String city,
    String postalCode,
    String country,
    Boolean active) implements BaseAddressRequest {
}
