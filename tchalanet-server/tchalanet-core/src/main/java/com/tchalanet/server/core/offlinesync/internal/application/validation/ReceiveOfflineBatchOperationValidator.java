package com.tchalanet.server.core.offlinesync.internal.application.validation;

import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiveOfflineBatchOperationValidator {

    private final QueryBus queryBus;

    public ValidatedReceiveOfflineBatchContext validate(
        OfflineBatchActorContext actor,
        OperationalRequestContext operationalContext
    ) {
        if (!operationalContext.isTrustedForSensitiveOperation()) {
            throw ProblemRest.forbidden("operational_context.untrusted");
        }

        var terminal = queryBus.ask(new ValidateTerminalForOperationQuery(
            actor.tenantId(),
            actor.terminalId(),
            actor.outletId(),
            actor.sellerUserId(),
            TerminalOperation.OFFLINE_SYNC
        ));

        var outlet = queryBus.ask(new ValidateOutletForOperationQuery(
            actor.tenantId(),
            actor.outletId(),
            OutletOperation.OFFLINE_SYNC
        ));

        var session = queryBus.ask(new ValidateSalesSessionForOperationQuery(
            actor.tenantId(),
            actor.salesSessionId(),
            actor.terminalId(),
            actor.outletId(),
            actor.sellerUserId(),
            SalesSessionOperation.OFFLINE_SYNC
        ));

        return new ValidatedReceiveOfflineBatchContext(
            actor.tenantId(),
            actor.sellerUserId(),
            actor.terminalId(),
            actor.outletId(),
            actor.salesSessionId(),
            terminal.displayCode(),
            outlet.outletName(),
            session.status(),
            session.closedAt(),
            session.finalized()
        );
    }
}
