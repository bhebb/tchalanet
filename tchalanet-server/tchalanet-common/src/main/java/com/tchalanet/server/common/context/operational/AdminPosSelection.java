package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;

public record AdminPosSelection(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    Instant expiresAt
) {
}
