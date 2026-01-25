package com.tchalanet.server.core.tenantconfig.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command: Archive Tenant (any → ARCHIVED).
 * Per command_query_handlers.md + core.tenant pattern: includes reason for audit trail.
 * Implements Command<Void> for VoidCommandHandler dispatch.
 */
public record ArchiveTenantCommand(
    @NotNull TenantId tenantId,
    String reason  // optional reason for archival
) implements Command<Void> {}
