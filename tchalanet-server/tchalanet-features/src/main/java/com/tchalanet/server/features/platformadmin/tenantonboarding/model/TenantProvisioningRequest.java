package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

import com.tchalanet.server.platform.tenant.api.model.TenantType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
    @NotNull
        @DecimalMin(value = "0.00", message = "defaultCommissionRate must be >= 0")
        @DecimalMax(value = "100.00", message = "defaultCommissionRate must be <= 100")
        BigDecimal defaultCommissionRate,
    @NotNull TenantProvisioningProfile profile,
    String initialAdminEmail) {}
