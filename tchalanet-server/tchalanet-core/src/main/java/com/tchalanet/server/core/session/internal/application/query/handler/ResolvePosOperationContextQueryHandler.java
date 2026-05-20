package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.core.session.api.query.ResolvePosOperationContextQuery;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.session.internal.application.service.PosActionPolicy;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResolvePosOperationContextQueryHandler
    implements QueryHandler<ResolvePosOperationContextQuery, ValidatedPosOperationContext> {

    private final QueryBus queryBus;
    private final PosActionPolicy policy;

    @Override
    public ValidatedPosOperationContext handle(ResolvePosOperationContextQuery query) {
        var hint = query.operationalContext();
        if (hint == null || !hint.hasPosFrame()
            || hint.terminalId() == null
            || hint.outletId() == null
            || hint.salesSessionId() == null) {
            throw ProblemRest.unprocessable("operational_context.pos_frame_required");
        }

        policy.assertAccepted(query.action(), hint.trust());

        var terminal = queryBus.ask(new ValidateTerminalForOperationQuery(
            query.tenantId(),
            hint.terminalId(),
            hint.outletId(),
            query.actorUserId(),
            policy.terminalOperation(query.action())));

        queryBus.ask(new ValidateOutletForOperationQuery(
            query.tenantId(),
            hint.outletId(),
            policy.outletOperation(query.action())));

        queryBus.ask(new ValidateSalesSessionForOperationQuery(
            query.tenantId(),
            hint.salesSessionId(),
            terminal.terminalId(),
            terminal.outletId(),
            query.actorUserId(),
            policy.salesSessionOperation(query.action())));

        return new ValidatedPosOperationContext(
            query.tenantId(),
            query.actorUserId(),
            terminal.terminalId(),
            terminal.outletId(),
            hint.salesSessionId(),
            hint.source(),
            hint.trust(),
            Instant.now());
    }
}
