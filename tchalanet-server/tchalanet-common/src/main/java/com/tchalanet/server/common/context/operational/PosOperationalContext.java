package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record PosOperationalContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    UserId sellerUserId,
    OperationalContextRole role,
    OperationalContextSource source,
    TrustLevel trustLevel
) implements OperationalRequestContext {
}
