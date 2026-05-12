package com.tchalanet.server.catalog.tenant.api.model;

import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

/**
 * Registry view for tenant listings and admin operations.
 * Per DOMAIN_TENANT_CATALOG.md: used for filters / admin listings.
 * Enhanced with currency, zoneId and typed ThemePresetId per typed_ids.md.
 * Uses TenantStatus and TenantType enums for type safety.
 */
public record TenantRegistryView(
    TenantId tenantId,
    String code,
    String name,
    TenantStatus status,                // enum: ACTIVE, SUSPENDED, ARCHIVED
    TenantType type,                    // enum: COMMERCIAL, PERSONAL
    ZoneId timezone,                    // tenant timezone
    Currency currency,                  // tenant currency
    Optional<AddressId> addressId,      // optional
    Optional<ThemePresetId> activeThemeId  // optional, typed ID
) {}
