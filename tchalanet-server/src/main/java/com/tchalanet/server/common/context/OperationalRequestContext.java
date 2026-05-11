package com.tchalanet.server.common.context;

import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;

/**
 * Carries terminal/outlet/session candidates for the current request.
 * Presence of these IDs does not prove the operation is allowed —
 * each sensitive use case calls its domain validator before mutation.
 */
public record OperationalRequestContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {

    public boolean hasTerminal() {
        return terminalId != null;
    }

    public boolean hasOutlet() {
        return outletId != null;
    }

    public boolean hasSalesSession() {
        return salesSessionId != null;
    }

    public boolean isTrustedForSensitiveOperation() {
        return source.isTrustedForSensitiveOperation();
    }

    public TerminalId terminalIdRequired() {
        if (terminalId == null) {
            throw ProblemRest.unprocessable("terminal.required: terminal is required");
        }
        return terminalId;
    }

    public OutletId outletIdRequired() {
        if (outletId == null) {
            throw ProblemRest.unprocessable("outlet.required: outlet is required");
        }
        return outletId;
    }

    public SalesSessionId salesSessionIdRequired() {
        if (salesSessionId == null) {
            throw ProblemRest.unprocessable("sales_session.required: sales session is required");
        }
        return salesSessionId;
    }

    public static OperationalRequestContext empty() {
        return new OperationalRequestContext(null, null, null, OperationalContextSource.NONE);
    }
}
