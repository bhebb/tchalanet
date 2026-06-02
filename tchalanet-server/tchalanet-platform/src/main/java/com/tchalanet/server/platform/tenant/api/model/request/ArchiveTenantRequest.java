package com.tchalanet.server.platform.tenant.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command: Archive Tenant (any → ARCHIVED).
 * Per command_query_handlers.md + core.tenant pattern: includes reason for audit trail.
 * Implements Command<Void> for VoidCommandHandler dispatch.
 */
public record ArchiveTenantRequest(
    @NotNull TenantId tenantId,
    String reason  // optional reason for archival
) {}
