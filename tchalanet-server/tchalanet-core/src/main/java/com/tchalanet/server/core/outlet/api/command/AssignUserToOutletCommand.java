package com.tchalanet.server.core.outlet.api.command.assignment;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record AssignUserToOutletCommand(
    TenantId tenantId, OutletId outletId, UserId userId, UserId actorUserId)
    implements Command<Void> {}
