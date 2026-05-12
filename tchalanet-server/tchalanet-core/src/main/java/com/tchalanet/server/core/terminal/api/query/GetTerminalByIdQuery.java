package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;

public record GetTerminalByIdQuery(
    TenantId tenantId,
    TerminalId terminalId
) implements Query<TerminalView> {}
