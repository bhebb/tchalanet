package com.tchalanet.server.core.session.internal.application.service;

import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.session.api.query.PosOperationAction;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import org.springframework.stereotype.Component;

@Component
public class PosActionPolicy {

    public void assertAccepted(PosOperationAction action, OperationalContextTrust trust) {
        if (minimumTrust(action) == OperationalContextTrust.STRONG && trust != OperationalContextTrust.STRONG) {
            throw ProblemRest.forbidden("operational_context.strong_trust_required");
        }
        if (trust == OperationalContextTrust.NONE) {
            throw ProblemRest.forbidden("operational_context.trust_required");
        }
    }

    public TerminalOperation terminalOperation(PosOperationAction action) {
        return switch (action) {
            case ADMIN_POS_SELL, SELL_TICKET_ONLINE -> TerminalOperation.SELL;
            case REQUEST_OFFLINE_GRANT -> TerminalOperation.OFFLINE_GRANT;
            case SYNC_OFFLINE_SALES -> TerminalOperation.OFFLINE_SYNC;
            case PAYOUT -> TerminalOperation.PAYOUT;
            case CLOSE_SESSION -> TerminalOperation.CANCEL;
        };
    }

    public OutletOperation outletOperation(PosOperationAction action) {
        return switch (action) {
            case ADMIN_POS_SELL, SELL_TICKET_ONLINE -> OutletOperation.SELL;
            case REQUEST_OFFLINE_GRANT -> OutletOperation.OFFLINE_GRANT;
            case SYNC_OFFLINE_SALES -> OutletOperation.OFFLINE_SYNC;
            case PAYOUT -> OutletOperation.PAYOUT;
            case CLOSE_SESSION -> OutletOperation.CANCEL;
        };
    }

    public SalesSessionOperation salesSessionOperation(PosOperationAction action) {
        return switch (action) {
            case ADMIN_POS_SELL, SELL_TICKET_ONLINE -> SalesSessionOperation.SELL;
            case REQUEST_OFFLINE_GRANT -> SalesSessionOperation.OFFLINE_GRANT;
            case SYNC_OFFLINE_SALES -> SalesSessionOperation.OFFLINE_SYNC;
            case PAYOUT -> SalesSessionOperation.PAYOUT;
            case CLOSE_SESSION -> SalesSessionOperation.CANCEL;
        };
    }

    private OperationalContextTrust minimumTrust(PosOperationAction action) {
        return switch (action) {
            case ADMIN_POS_SELL, SELL_TICKET_ONLINE -> OperationalContextTrust.WEAK;
            case REQUEST_OFFLINE_GRANT, SYNC_OFFLINE_SALES, PAYOUT, CLOSE_SESSION -> OperationalContextTrust.STRONG;
        };
    }
}
