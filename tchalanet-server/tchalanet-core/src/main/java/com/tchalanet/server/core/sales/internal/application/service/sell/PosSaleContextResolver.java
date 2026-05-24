package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.core.session.api.query.PosOperationAction;
import com.tchalanet.server.core.session.api.query.ResolvePosOperationContextQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PosSaleContextResolver {

    private final QueryBus queryBus;

    // -------------------------------------------------------------------------
    // POS
    // -------------------------------------------------------------------------

    public ValidatedPosOperationContext resolve(TchRequestContext ctx) {
        return queryBus.ask(new ResolvePosOperationContextQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.userId(),
            ctx.operationalContextRequired(),
            PosOperationAction.SELL_TICKET_ONLINE
        ));
    }

}
