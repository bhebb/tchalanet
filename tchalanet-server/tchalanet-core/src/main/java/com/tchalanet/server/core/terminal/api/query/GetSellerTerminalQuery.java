package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalView;

public record GetSellerTerminalQuery(
    TenantId tenantId,
    SellerTerminalId terminalId
) implements Query<SellerTerminalView> {}
