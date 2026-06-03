package com.tchalanet.server.platform.tenant.api.model.view;

import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.address.api.model.AddressView;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Admin view for tenant — returned to SUPER_ADMIN and TENANT_ADMIN.
 * Does not expose raw config JSON, deleted_at, or internal metadata.
 */
public record TenantAdminView(
    TenantId tenantId,
    String code,
    String name,
    TenantType type,
    TenantStatus status,
    ZoneId timezone,
    Currency currency,
    String defaultLanguage,
    String defaultLocale,
    ThemePresetId activeThemeId,
    String activeThemeCode,
    AddressView address,
    Instant createdAt,
    Instant updatedAt
) {}
