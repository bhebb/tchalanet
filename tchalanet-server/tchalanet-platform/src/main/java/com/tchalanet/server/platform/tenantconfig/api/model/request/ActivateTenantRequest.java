package com.tchalanet.server.platform.tenantconfig.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command: Activate Tenant (DRAFT → ACTIVE).
 * Per command_query_handlers.md + core.tenant pattern.
 * Implements Command<Void> for VoidCommandHandler dispatch.
 */
public record ActivateTenantRequest(
    @NotNull TenantId tenantId
) {}
