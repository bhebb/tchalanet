package com.tchalanet.server.core.tenantuser.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.types.enums.AutonomyLevel;

public record AssignUserToTenantCommand(
    TenantId tenantId,
    UserId userId,
    RoleId roleId,
    OutletId outletId,
    TerminalId terminalId,
    boolean isOwner) implements Command<AssignUserToTenantResult> {}
