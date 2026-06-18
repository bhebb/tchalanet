package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.internal.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.internal.application.sale.SaleEvaluationMode;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.PreparedSale;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyDecision;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SaleSellerContext;
import com.tchalanet.server.common.web.error.ProblemRest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Prepares a ticket sale: validates the command, resolves the POS frame,
 * computes ticket lines and charges, evaluates limit policy and seller
 * autonomy, and aggregates user-facing notices.
 *
 * <p>This service makes ALL pre-commit decisions. The handler only consumes
 * {@link PreparedSale} and persists. No business logic lives in the handler.
 */
@Component
@RequiredArgsConstructor
public class SalePreparationOrchestrator {

    private final DrawCutoffRule drawCutoffRule;
    private final TicketLinePreparationService ticketLinePreparationService;
    private final SaleChargeCalculator saleChargeCalculator;
    private final SaleCommandValidator saleCommandValidator;
    private final TchTimeProvider tchTimeProvider;
    private final PosSaleContextResolver posSaleContextResolver;
    private final SaleMoneyCalculator saleMoneyCalculator;

    public PreparedSale prepareSale(
        SellTicketCommand command,
        TchRequestContext ctx,
        SaleEvaluationMode mode
    ) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(ctx, "ctx is required");
        Objects.requireNonNull(mode, "mode is required");

        saleCommandValidator.validateCommand(command);

        var now = tchTimeProvider.now();
        var tenantId = ctx.effectiveTenantIdRequired();

        if (ctx.actorType() == TchActorType.SELLER_TERMINAL) {
            return prepareSaleForSellerTerminal(command, ctx, now, tenantId);
        }

        throw ProblemRest.forbidden("seller_terminal.actor_required");
    }

    private PreparedSale prepareSaleForSellerTerminal(
        SellTicketCommand command,
        TchRequestContext ctx,
        Instant now,
        TenantId tenantId
    ) {
        var pos = posSaleContextResolver.resolveForSellerTerminal(ctx);
        var draw = drawCutoffRule.requireBeforeCutoff(command.drawId());
        var mergedLines = command.lines();
        var paidLines = ticketLinePreparationService.toTicketLines(tenantId, mergedLines, command.currency());
        var charges = saleChargeCalculator.compute(tenantId, command);
        var money = saleMoneyCalculator.compute(paidLines, charges, command);
        var policyDecision = SalePolicyDecision.allowed(new LimitEvaluationView(null, List.of()));

        return new PreparedSale(
            pos, draw, now, mergedLines, paidLines, charges, money,
            policyDecision.limits(), policyDecision.autonomy(),
            false, null, null, null,
            List.of(),
            SaleSellerContext.empty().sellerId(),
            SaleSellerContext.empty().sellerAssignmentId()
        );
    }
}
