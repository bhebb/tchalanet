package com.tchalanet.server.platform.tenant.api.model.view;

import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.address.api.model.AddressView;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Tenant configuration view for queries.
 * Per user request: includes both activeThemeId (for reference) and activeThemeCode (for display).
 */
public record TenantConfigView(
    TenantId tenantId,
    String code,
    String name,
    TenantType type,
    ZoneId timezone,
    Currency currency,
    TenantStatus status,
    ThemePresetId activeThemeId,      // ID for reference/operations
    String activeThemeCode,            // code for display (e.g. "modern-light")
    AddressView address,
    JsonNode internalSettings,
    BigDecimal defaultCommissionRate,
    Instant createdAt,
    Instant updatedAt
) {}
