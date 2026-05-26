package com.tchalanet.server.core.outlet.api.command.block;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public record BlockOutletCommand(
    TenantId tenantId,
    OutletId outletId,
    String reason
) implements Command<Void> {}
