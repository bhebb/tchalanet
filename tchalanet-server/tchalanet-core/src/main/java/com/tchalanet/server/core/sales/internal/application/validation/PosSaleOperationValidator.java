package com.tchalanet.server.core.sales.internal.application.validation;

import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosSaleOperationValidator {

    private final QueryBus queryBus;

    public ValidatedPosSaleContext validate(
        PosOperationActorContext actor,
        OperationalRequestContext operationalContext
    ) {
        ensureTrusted(operationalContext);

        var terminal = queryBus.ask(new ValidateTerminalForOperationQuery(
            actor.tenantId(),
            actor.terminalId(),
            actor.outletId(),
            actor.actorUserId(),
            TerminalOperation.SELL
        ));

        var outlet = queryBus.ask(new ValidateOutletForOperationQuery(
            actor.tenantId(),
            actor.outletId(),
            OutletOperation.SELL
        ));

        var session = queryBus.ask(new ValidateSalesSessionForOperationQuery(
            actor.tenantId(),
            actor.salesSessionId(),
            actor.terminalId(),
            actor.outletId(),
            actor.actorUserId(),
            SalesSessionOperation.SELL
        ));

        return new ValidatedPosSaleContext(
            actor.tenantId(),
            actor.actorUserId(),
            actor.terminalId(),
            actor.outletId(),
            actor.salesSessionId(),
            terminal.displayCode(),
            outlet.outletName(),
            session.openedAt()
        );
    }

    private void ensureTrusted(OperationalRequestContext ctx) {
        if (!ctx.isTrustedForSensitiveOperation()) {
            throw ProblemRest.forbidden("operational_context.untrusted");
        }
    }
}
