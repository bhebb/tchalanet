package com.tchalanet.server.platform.tenantconfig.api.model.request;

import com.tchalanet.server.catalog.tenant.api.model.TenantType;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.address.api.model.AddressInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZoneId;
import java.util.Currency;

/**
 * Command: Create a new tenant.
 * Per DOMAIN_TENANT_CONFIG.md + command_query_handlers.md:
 * - Required: code, name, type, timezone, currency
 * - Optional: address, activeThemeId
 * - Optional: activate flag (default false = DRAFT, true = ACTIVE)
 * - Implements Command<Void> for VoidCommandHandler dispatch
 * - Uses ZoneId and Currency types for type-safety
 */
public record CreateTenantRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotNull TenantType type,
    @NotNull ZoneId timezone,
    @NotNull Currency currency,
    AddressInput address,
    ThemePresetId activeThemeId,  // optional theme preset
    Boolean activate              // optional: true = ACTIVE, false/null = DRAFT
) {}
