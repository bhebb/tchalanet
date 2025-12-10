package com.tchalanet.server.common.domain;

public record Address(
    String line1,
    String line2,
    String city,
    String region,
    String country,
    String postalCode,
    Double latitude,
    Double longitude
) {}
