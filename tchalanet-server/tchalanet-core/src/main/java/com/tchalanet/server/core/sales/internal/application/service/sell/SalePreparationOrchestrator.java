package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.internal.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.internal.application.sale.SaleEvaluationMode;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.PreparedSale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    private final SaleLimitAutonomyEvaluator saleLimitAutonomyEvaluator;
    private final SalePromotionEvaluator salePromotionEvaluator;
    private final SaleCommandValidator saleCommandValidator;
    private final IdGenerator idGenerator;
    private final TchTimeProvider tchTimeProvider;
    private final PosSaleContextResolver posSaleContextResolver;
    private final SaleMoneyCalculator saleMoneyCalculator;
    private final  PosSaleContextResolver saleAgentPromotionContextResolver;

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

        var pos = posSaleContextResolver.resolve(ctx);

        var draw = drawCutoffRule.requireBeforeCutoff(command.drawId());

        // V1: no normalize/merge; restore canonical merge here later.
        var mergedLines = command.lines();

        var ticketLines = ticketLinePreparationService.toTicketLines(
            tenantId,
            mergedLines,
            command.currency()
        );

        var charges = saleChargeCalculator.compute(tenantId, command);
        var moneyBreakdown = saleMoneyCalculator.compute(ticketLines, charges, command);

        var agentPromotionContext = saleAgentPromotionContextResolver.resolve(pos);

        var paidLines = ticketLinePreparationService.toTicketLines(
            tenantId,
            mergedLines,
            command.currency()
        );

        var initialCharges = saleChargeCalculator.compute(tenantId, command);
        var initialMoney = saleMoneyCalculator.compute(paidLines, initialCharges, command);

        var promotionDecision = salePromotionEvaluator.evaluate(
            tenantId,
            command,
            pos,
            now,
            initialMoney.total(),
            mode
        );

        var appliedPromotion = salePromotionEffectApplier.apply(
            promotionDecision,
            paidLines,
            initialCharges,
            command,
            command.currency()
        );

        var finalLines = appliedPromotion.ticketLines();
        var finalCharges = appliedPromotion.charges();

        var finalMoney = saleMoneyCalculator.compute(finalLines, finalCharges, command);

        var policyDecision = saleLimitAutonomyEvaluator.evaluate(
            tenantId,
            command,
            pos,
            draw,
            toLimitInputs(finalLines),
            now
        );

        var policyDecision = saleLimitAutonomyEvaluator.evaluate(
            tenantId,
            command,
            pos,
            draw,
            mergedLines,
            now
        );

        var notices = SalesNoticeAssembler.assemble(
            policyDecision,
            charges,
            promotionDecision
        );

        var approvalRequestId = policyDecision.requiresApproval()
            ? ApprovalRequestId.of(idGenerator.newUuid())
            : null;

        return new PreparedSale(
            pos,
            draw,
            now,
            mergedLines,
            ticketLines,
            charges,
            moneyBreakdown,
            policyDecision.limits(),
            policyDecision.autonomy(),
            policyDecision.requiresApproval(),
            policyDecision.approvalLevel(),
            approvalRequestId,
            promotionDecision,
            List.copyOf(notices)
        );
    }
}
