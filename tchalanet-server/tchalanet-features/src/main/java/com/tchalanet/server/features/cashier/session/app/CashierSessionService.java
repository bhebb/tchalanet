package com.tchalanet.server.features.cashier.session.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.session.api.command.CloseSalesSessionCommand;
import com.tchalanet.server.core.session.api.command.OpenSalesSessionCommand;
import com.tchalanet.server.core.session.api.query.GetCurrentSalesSessionQuery;
import com.tchalanet.server.features.cashier.session.model.CashierSessionView;
import com.tchalanet.server.features.cashier.session.model.CloseCashierSessionRequest;
import com.tchalanet.server.features.cashier.session.model.OpenCashierSessionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierSessionService {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public CashierSessionView current(TchRequestContext ctx, TerminalId terminalId) {
        var optional = queryBus.ask(new GetCurrentSalesSessionQuery(
            ctx.effectiveTenantIdRequired(),
            terminalId
        ));
        return optional.map(CashierSessionView::from).orElse(null);
    }

    public CashierSessionView open(TchRequestContext ctx, OpenCashierSessionRequest request) {
        var result = commandBus.execute(new OpenSalesSessionCommand(
            ctx.effectiveTenantIdRequired(),
            request.outletId(),
            request.terminalId(),
            ctx.userId(),
            toCents(request.openingFloat())
        ));
        return new CashierSessionView(
            result.sessionId(),
            request.outletId(),
            request.terminalId(),
            ctx.userId(),
            "OPEN",
            result.openedAt(),
            null,
            request.openingFloat(),
            null,
            null
        );
    }

    public CashierSessionView close(TchRequestContext ctx, CloseCashierSessionRequest request) {
        var result = commandBus.execute(new CloseSalesSessionCommand(
            ctx.effectiveTenantIdRequired(),
            request.sessionId(),
            toCents(request.closingAmount()),
            ctx.userId(),
            request.reason()
        ));
        return new CashierSessionView(
            result.sessionId(),
            null,
            null,
            null,
            "CLOSED",
            null,
            result.closedAt(),
            null,
            ctx.userId(),
            request.closingAmount()
        );
    }

    private static long toCents(java.math.BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
