package com.tchalanet.server.platform.tenant.api.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

/**
 * Tenant Context Lookup View for tenant listings and admin operations.
 */
public record TenantContextLookupView(
    TenantId tenantId,
    String code,
    String name,
    TenantStatus status,
    TenantType type,
    ZoneId timezone,
    Currency currency,
    String defaultLanguage,
    String defaultLocale,
    Optional<AddressId> addressId,
    Optional<ThemePresetId> activeThemeId,
    Optional<BigDecimal> defaultCommissionRate
) {
}
