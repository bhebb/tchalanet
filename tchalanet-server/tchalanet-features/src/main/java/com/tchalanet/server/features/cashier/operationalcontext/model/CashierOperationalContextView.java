package com.tchalanet.server.features.cashier.operationalcontext.model;

import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;

public record CashierOperationalContextView(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source,
    OperationalContextTrust trust
) {
    public static CashierOperationalContextView from(OperationalContextHint h) {
        if (h == null || !h.hasPosFrame()) {
            return null;
        }
        return new CashierOperationalContextView(
            h.terminalId(),
            h.outletId(),
            h.salesSessionId(),
            h.source(),
            h.trust()
        );
    }
}
