package com.tchalanet.server.core.address.application.dto;

import java.util.UUID;

public record AddressDto(
    UUID id,
    String line1,
    String line2,
    String city,
    String region,
    String country,
    String postalCode,
    Double latitude,
    Double longitude) {}
