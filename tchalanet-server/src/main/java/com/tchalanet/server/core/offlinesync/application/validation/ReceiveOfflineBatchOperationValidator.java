package com.tchalanet.server.core.offlinesync.application.validation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.core.offlinesync.application.validation.OfflineBatchActorContext;
import com.tchalanet.server.core.offlinesync.application.validation.ValidatedReceiveOfflineBatchContext;
import com.tchalanet.server.core.outlet.application.query.model.OutletOperation;
import com.tchalanet.server.core.outlet.application.query.model.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.application.query.model.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.session.domain.model.SalesSessionOperation;
import com.tchalanet.server.core.terminal.application.query.model.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.domain.model.TerminalOperation;
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
