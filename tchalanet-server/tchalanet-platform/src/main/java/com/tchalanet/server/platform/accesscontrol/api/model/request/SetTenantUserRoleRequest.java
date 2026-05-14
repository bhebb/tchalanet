package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.security.TchRole;

/**
 * Command to set the tenant-scoped role for a user (mono-role per tenant).
 */
public record SetTenantUserRoleRequest(
    TenantId tenantId,
    UserId userId,
    TchRole role
) {}


