package com.tchalanet.server.core.tenantconfig.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.core.address.application.model.AddressInput;
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
public record CreateTenantCommand(
    @NotBlank String code,
    @NotBlank String name,
    @NotNull TenantType type,
    @NotNull ZoneId timezone,
    @NotNull Currency currency,
    AddressInput address,
    ThemePresetId activeThemeId,  // optional theme preset
    Boolean activate              // optional: true = ACTIVE, false/null = DRAFT
) implements Command<Void> {}
