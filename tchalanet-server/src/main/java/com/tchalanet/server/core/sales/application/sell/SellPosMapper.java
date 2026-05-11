package com.tchalanet.server.core.sales.application.sell;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sales.infra.web.model.SellTicketRequest;

public class SellPosMapper {
    public SellPosTicketCommand toCommand(TchRequestContext ctx, SellTicketRequest request) {
        return new SellPosTicketCommand(
            ctx.effectiveTenantIdRequired(),
            ctx.currentUserIdRequired(),
            ctx.terminalIdRequired(),
            ctx.outletIdRequired(),
            ctx.salesSessionIdRequired(),
            ctx.operationalContext(),
            request.drawId(),
            ctx.effectiveTenantIdRequired(),
            request.feeAmount(),
            request.lines().stream().map(this::toLineInput).toList()
        );
    }
}
