package com.tchalanet.server.core.sales.internal.application.validation;

import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosCancelOperationValidator {

    private final QueryBus queryBus;

    public ValidatedPosCancelContext validate(
        PosOperationActorContext actor,
        OperationalRequestContext operationalContext
    ) {
        if (!operationalContext.isTrustedForSensitiveOperation()) {
            throw ProblemRest.forbidden("operational_context.untrusted");
        }

        var terminal = queryBus.ask(new ValidateTerminalForOperationQuery(
            actor.tenantId(),
            actor.terminalId(),
            actor.outletId(),
            actor.actorUserId(),
            TerminalOperation.CANCEL
        ));

        var session = queryBus.ask(new ValidateSalesSessionForOperationQuery(
            actor.tenantId(),
            actor.salesSessionId(),
            actor.terminalId(),
            actor.outletId(),
            actor.actorUserId(),
            SalesSessionOperation.CANCEL
        ));

        return new ValidatedPosCancelContext(
            actor.tenantId(),
            actor.actorUserId(),
            actor.terminalId(),
            actor.outletId(),
            actor.salesSessionId(),
            terminal.displayCode(),
            session.openedAt()
        );
    }
}
