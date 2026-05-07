package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record BlockOutletSalesCommand(
    TenantId tenantId, OutletId outletId, String reason, UserId actorUserId)
    implements Command<Void> {}
