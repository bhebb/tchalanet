package com.tchalanet.server.core.terminal.api.query;


import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;

public record CurrentOperationalContextView(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {
}
