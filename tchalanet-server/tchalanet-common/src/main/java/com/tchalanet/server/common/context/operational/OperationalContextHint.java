package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;

public record OperationalContextHint(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source,
    OperationalContextTrust trust
) {

    public boolean hasPosFrame() {
        return terminalId != null || outletId != null || salesSessionId != null;
    }

    public boolean trustedForSensitiveOperation() {
        return trust == OperationalContextTrust.STRONG;
    }
}
