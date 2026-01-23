package com.tchalanet.server.catalog.address.api;

public record AddressSearchCriteria(
    String line1,
    String city,
    String postalCode,
    String country,
    String outletCode,
    Boolean active) {

    public static AddressSearchCriteria empty() {
        return new AddressSearchCriteria(null, null, null, null, null, null);
    }
}
