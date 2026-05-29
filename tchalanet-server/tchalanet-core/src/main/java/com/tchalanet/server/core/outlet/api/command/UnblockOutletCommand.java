package com.tchalanet.server.core.outlet.api.command.block;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public record UnblockOutletCommand(
    TenantId tenantId,
    OutletId outletId
) implements Command<Void> {}
