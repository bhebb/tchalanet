package com.tchalanet.server.core.tenantconfig.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command: Deactivate Tenant (ACTIVE → SUSPENDED).
 * Per command_query_handlers.md + core.tenant pattern: includes reason for audit trail.
 * Alias for Suspend with explicit naming per core.tenant.
 */
public record DeactivateTenantCommand(
    @NotNull TenantId tenantId,
    String reason  // optional reason for deactivation
) implements Command<Void> {}
