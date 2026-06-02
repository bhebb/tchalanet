package com.tchalanet.server.platform.identity.api.model.request;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Request to provision an initial user for a freshly created tenant.
 * Used by the tenant provisioning orchestrator.
 */
public record ProvisionTenantUserRequest(
    TenantId tenantId,
    String tenantCode,
    String email,
    String firstName,
    String lastName,
    TchRole role,
    String tempPassword
) {}
