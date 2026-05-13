package com.tchalanet.server.core.payout.internal.application.validation;

import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionOperation;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosPayoutOperationValidator {

    private final QueryBus queryBus;

    public ValidatedPosPayoutContext validate(
        TenantId tenantId,
        UserId actorUserId,
        TerminalId terminalId,
        OutletId outletId,
        SalesSessionId salesSessionId,
        OperationalRequestContext operationalContext
    ) {
        if (!operationalContext.isTrustedForSensitiveOperation()) {
            throw ProblemRest.forbidden("operational_context.untrusted");
        }

        var terminal = queryBus.ask(new ValidateTerminalForOperationQuery(
            tenantId, terminalId, outletId, actorUserId, TerminalOperation.PAYOUT
        ));

        var outlet = queryBus.ask(new ValidateOutletForOperationQuery(
            tenantId, outletId, OutletOperation.PAYOUT
        ));

        var session = queryBus.ask(new ValidateSalesSessionForOperationQuery(
            tenantId, salesSessionId, terminalId, outletId, actorUserId, SalesSessionOperation.PAYOUT
        ));

        return new ValidatedPosPayoutContext(
            tenantId,
            actorUserId,
            terminalId,
            outletId,
            salesSessionId,
            terminal.displayCode(),
            outlet.outletName(),
            session.openedAt()
        );
    }
}
