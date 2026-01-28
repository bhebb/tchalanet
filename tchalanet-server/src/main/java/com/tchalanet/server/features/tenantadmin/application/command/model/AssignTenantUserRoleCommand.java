package com.tchalanet.server.features.tenantadmin.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.RoleId;

public record AssignTenantUserRoleCommand(TenantId tenantId, UserId userId, RoleId roleId) implements Command<Void> {}
