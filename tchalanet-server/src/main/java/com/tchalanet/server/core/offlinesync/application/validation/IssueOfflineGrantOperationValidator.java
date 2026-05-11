package com.tchalanet.server.core.offlinesync.application.validation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.core.offlinesync.application.validation.OfflineGrantActorContext;
import com.tchalanet.server.core.offlinesync.application.validation.ValidatedOfflineGrantContext;
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
public class IssueOfflineGrantOperationValidator {

    private final QueryBus queryBus;

    public ValidatedOfflineGrantContext validate(
        OfflineGrantActorContext actor,
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
            TerminalOperation.OFFLINE_GRANT
        ));

        var outlet = queryBus.ask(new ValidateOutletForOperationQuery(
            actor.tenantId(),
            actor.outletId(),
            OutletOperation.OFFLINE_GRANT
        ));

        var session = queryBus.ask(new ValidateSalesSessionForOperationQuery(
            actor.tenantId(),
            actor.salesSessionId(),
            actor.terminalId(),
            actor.outletId(),
            actor.sellerUserId(),
            SalesSessionOperation.OFFLINE_GRANT
        ));

        return new ValidatedOfflineGrantContext(
            actor.tenantId(),
            actor.sellerUserId(),
            actor.terminalId(),
            actor.outletId(),
            actor.salesSessionId(),
            terminal.displayCode(),
            outlet.outletName(),
            session.openedAt()
        );
    }
}
