package com.tchalanet.server.core.tenantuser.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;

public record AssignUserToTenantCommand(
    TenantId tenantId,
    UserId userId,
    RoleId roleId,
    AutonomyLevel autonomyLevel,
    boolean isOwner) implements Command<AssignUserToTenantResult> {}
