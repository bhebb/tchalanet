package com.tchalanet.server.core.outlet.api.command.block;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record UnblockOutletSalesCommand(
    TenantId tenantId, OutletId outletId, UserId actorUserId) implements Command<Void> {}
