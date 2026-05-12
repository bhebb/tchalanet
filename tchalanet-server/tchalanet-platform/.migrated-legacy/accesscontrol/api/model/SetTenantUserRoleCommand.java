package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.enums.TchRole;

/**
 * Command to set the tenant-scoped role for a user (mono-role per tenant).
 */
public record SetTenantUserRoleRequest(
    TenantId tenantId,
    UserId userId,
    TchRole role
) implements Command<Void> {}

