package com.tchalanet.server.core.terminal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.application.query.model.ValidatedTerminalOperationView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidateTerminalForOperationQueryHandler
    implements QueryHandler<ValidateTerminalForOperationQuery, ValidatedTerminalOperationView> {

    private final TerminalReaderPort terminalReader;

    @Override
    public ValidatedTerminalOperationView handle(ValidateTerminalForOperationQuery q) {
        var terminal = terminalReader.getRequired(q.tenantId(), q.terminalId());

        if (!terminal.tenantId().equals(q.tenantId())) {
            throw ProblemRest.forbidden("terminal.tenant_mismatch");
        }

        if (!terminal.outletId().equals(q.outletId())) {
            throw ProblemRest.forbidden("terminal.outlet_mismatch");
        }

        if (!terminal.assignedTo(q.actorUserId())) {
            throw ProblemRest.forbidden("terminal.seller_not_assigned");
        }

        if (terminal.locked()) {
            throw ProblemRest.forbidden("terminal.locked");
        }

        switch (q.operation()) {
            case SELL -> {
                if (terminal.salesBlocked()) throw ProblemRest.forbidden("terminal.sales_blocked");
            }
            case PAYOUT -> {
                if (terminal.payoutBlocked()) throw ProblemRest.forbidden("terminal.payout_blocked");
            }
            case OFFLINE_GRANT -> {
                if (terminal.salesBlocked()) throw ProblemRest.forbidden("terminal.sales_blocked_for_offline_grant");
                if (terminal.offlineBlocked()) throw ProblemRest.forbidden("terminal.offline_blocked");
            }
            case CANCEL -> {
                // ticket policy decides whether terminal may cancel
            }
            case OFFLINE_SYNC -> {
                // Receive for audit if not locked. If locked should be blocked above.
            }
        }

        return new ValidatedTerminalOperationView(
            terminal.id(),
            terminal.outletId(),
            terminal.assignedUserId(),
            terminal.displayCode(),
            terminal.state(),
            terminal.locked(),
            terminal.salesBlocked(),
            terminal.payoutBlocked(),
            terminal.offlineBlocked());
    }
}
