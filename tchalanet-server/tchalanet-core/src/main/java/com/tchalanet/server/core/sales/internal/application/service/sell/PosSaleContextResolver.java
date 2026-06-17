package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.core.session.api.query.PosOperationAction;
import com.tchalanet.server.core.session.api.query.ResolvePosOperationContextQuery;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalForSaleValidationQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
@RequiredArgsConstructor
public class PosSaleContextResolver {

    private final QueryBus queryBus;

    // -------------------------------------------------------------------------
    // Legacy POS path (Cashier + Session + Terminal)
    // -------------------------------------------------------------------------

    public ValidatedPosOperationContext resolve(TchRequestContext ctx) {
        var pos = queryBus.ask(new ResolvePosOperationContextQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.userId(),
            ctx.trustedOperationalContextRequired(),
            PosOperationAction.SELL_TICKET_ONLINE
        ));

        queryBus.ask(new ValidateTerminalForOperationQuery(
            pos.tenantId(),
            pos.terminalId(),
            pos.outletId(),
            pos.actorUserId(),
            TerminalOperation.SELL_TICKET
        ));

        return pos;
    }

    // -------------------------------------------------------------------------
    // SellerTerminal path — validates terminal status, returns synthetic pos
    // -------------------------------------------------------------------------

    public ValidatedPosOperationContext resolveForSellerTerminal(TchRequestContext ctx) {
        var terminalId = ctx.sellerTerminalId();
        if (terminalId == null) {
            throw ProblemRest.forbidden("seller_terminal.id_required");
        }

        var terminal = queryBus.ask(new GetSellerTerminalForSaleValidationQuery(
            ctx.effectiveTenantIdRequired(), terminalId));

        if (!terminal.canSell()) {
            throw ProblemRest.forbidden("seller_terminal.cannot_sell");
        }

        return new ValidatedPosOperationContext(
            ctx.effectiveTenantIdRequired(),
            null,
            null,
            null,
            null,
            OperationalContextSource.NONE,
            OperationalContextTrust.STRONG,
            Instant.now()
        );
    }
}
