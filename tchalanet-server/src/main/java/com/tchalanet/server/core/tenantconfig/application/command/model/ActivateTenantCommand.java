package com.tchalanet.server.core.tenantconfig.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command: Activate Tenant (DRAFT → ACTIVE).
 * Per command_query_handlers.md + core.tenant pattern.
 * Implements Command<Void> for VoidCommandHandler dispatch.
 */
public record ActivateTenantCommand(
    @NotNull TenantId tenantId
) implements Command<Void> {}
