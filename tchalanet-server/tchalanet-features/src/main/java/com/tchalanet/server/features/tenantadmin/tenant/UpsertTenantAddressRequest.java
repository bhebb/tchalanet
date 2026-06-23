package com.tchalanet.server.features.tenantadmin.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertTenantAddressRequest(
    @NotBlank @Size(max = 200) String line1,
    @Size(max = 200) String line2,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 100) String region,
    @NotBlank @Size(max = 2) String country,
    @Size(max = 20) String postalCode
) {}
