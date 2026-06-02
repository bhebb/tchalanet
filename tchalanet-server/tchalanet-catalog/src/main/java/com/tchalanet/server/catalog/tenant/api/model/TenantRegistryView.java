package com.tchalanet.server.catalog.tenant.api.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

/**
 * Registry view for tenant listings and admin operations.
 */
public record TenantRegistryView(
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
    Optional<ThemePresetId> activeThemeId
) {}
