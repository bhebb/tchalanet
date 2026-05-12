package com.tchalanet.server.core.terminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSalesSessionQuery;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.CurrentOperationalContextView;
import com.tchalanet.server.core.terminal.application.query.model.GetCurrentOperationalContextQuery;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentOperationalContextQueryHandler
    implements QueryHandler<GetCurrentOperationalContextQuery, CurrentOperationalContextView> {

    private final TerminalReaderPort terminalReader;
    private final QueryBus queryBus;

    @Override
    public CurrentOperationalContextView handle(GetCurrentOperationalContextQuery q) {
        // v1: SERVER_BOOTSTRAP — resolve from user's currently assigned terminal.
        // Future phases: add SIGNED_DEVICE_BINDING (terminalBinding header) and
        // ADMIN_SELECTION (short-TTL selection row) resolution here.
        var terminal = terminalReader
            .findCurrentForUser(q.userId())
            .filter(t -> t.tenantId().equals(q.tenantId()))
            .orElse(null);

        if (terminal == null) {
            return emptyView();
        }

        var sessionId = queryBus.ask(new GetCurrentSalesSessionQuery(q.tenantId(), terminal.id()))
            .map(SalesSession::id)
            .orElse(null);

        return new CurrentOperationalContextView(
            terminal.id(),
            terminal.outletId(),
            sessionId,
            OperationalContextSource.SERVER_BOOTSTRAP
        );
    }

    private static CurrentOperationalContextView emptyView() {
        return new CurrentOperationalContextView(null, null, null, OperationalContextSource.NONE);
    }
}
