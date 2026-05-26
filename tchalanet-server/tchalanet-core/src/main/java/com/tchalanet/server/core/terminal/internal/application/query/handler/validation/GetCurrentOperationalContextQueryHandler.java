package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.query.CurrentOperationalContextView;
import com.tchalanet.server.core.terminal.api.query.GetCurrentOperationalContextQuery;

@UseCase
public class GetCurrentOperationalContextQueryHandler
    implements QueryHandler<GetCurrentOperationalContextQuery, CurrentOperationalContextView> {

    @Override
    public CurrentOperationalContextView handle(GetCurrentOperationalContextQuery query) {
        var context = query.operationalContext();
        if (context == null || !context.hasPosFrame()) {
            return new CurrentOperationalContextView(
                null,
                null,
                null,
                OperationalContextSource.NONE,
                OperationalContextTrust.NONE,
                false,
                false);
        }

        return new CurrentOperationalContextView(
            context.terminalId(),
            context.outletId(),
            context.salesSessionId(),
            context.source(),
            context.trust(),
            true,
            context.trustedForSensitiveOperation());
    }
}
