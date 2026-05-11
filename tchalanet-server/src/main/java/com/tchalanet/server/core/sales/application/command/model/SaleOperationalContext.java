package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record SaleOperationalContext(
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId
) {}
