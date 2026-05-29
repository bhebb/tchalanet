package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

import com.tchalanet.server.catalog.tenant.api.model.TenantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Shared request payload for {@code POST /platform/tenant-onboarding/preview}
 * and {@code POST /platform/tenant-onboarding/provision}.
 */
public record TenantProvisioningRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotNull TenantType type,
    @NotNull ZoneId timezone,
    @NotNull Currency currency,
    @NotNull TenantProvisioningProfile profile,
    String initialAdminEmail) {}
