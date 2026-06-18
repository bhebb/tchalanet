package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalForSaleValidationQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
@RequiredArgsConstructor
public class PosSaleContextResolver {

    private final QueryBus queryBus;

    public ValidatedPosOperationContext resolveForSellerTerminal(TchRequestContext ctx) {
        var terminalId = ctx.sellerTerminalIdRequired();
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
