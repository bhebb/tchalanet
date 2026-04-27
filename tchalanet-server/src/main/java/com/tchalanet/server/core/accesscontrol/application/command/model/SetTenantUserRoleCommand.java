package com.tchalanet.server.core.accesscontrol.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.enums.TchRole;

/**
 * Command to set the tenant-scoped role for a user (mono-role per tenant).
 */
public record SetTenantUserRoleCommand(
    TenantId tenantId,
    UserId userId,
    TchRole role
) implements Command<Void> {}
