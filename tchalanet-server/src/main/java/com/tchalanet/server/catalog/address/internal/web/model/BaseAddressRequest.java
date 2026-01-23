package com.tchalanet.server.catalog.address.internal.web.model;

public sealed interface BaseAddressRequest permits CreateAddressRequest, UpdateAddressRequest {
    String line1();

    String line2();

    String city();

    String postalCode();

    String country();

    Boolean active();
}
