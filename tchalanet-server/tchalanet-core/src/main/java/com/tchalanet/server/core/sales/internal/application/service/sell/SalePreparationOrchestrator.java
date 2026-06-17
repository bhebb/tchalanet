package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.internal.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.internal.application.sale.SaleEvaluationMode;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.AppliedSalePromotionEffects;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.PreparedSale;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyDecision;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyInput;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SaleSellerContext;
import com.tchalanet.server.core.sales.internal.application.service.sell.promotion.SalePromotionEffectApplier;
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
    private final SaleLimitAutonomyEvaluator saleLimitAutonomyEvaluator;
    private final SalePromotionEvaluator salePromotionEvaluator;
    private final SalePromotionEffectApplier salePromotionEffectApplier;
    private final SaleCommandValidator saleCommandValidator;
    private final IdGenerator idGenerator;
    private final TchTimeProvider tchTimeProvider;
    private final PosSaleContextResolver posSaleContextResolver;
    private final SaleMoneyCalculator saleMoneyCalculator;
    private final SaleSellerContextResolver saleSellerContextResolver;

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

        var pos = posSaleContextResolver.resolve(ctx);

        var draw = drawCutoffRule.requireBeforeCutoff(command.drawId());

        var mergedLines = command.lines();

        var paidLines = ticketLinePreparationService.toTicketLines(tenantId, mergedLines, command.currency());
        var initialCharges = saleChargeCalculator.compute(tenantId, command);
        var initialMoney = saleMoneyCalculator.compute(paidLines, initialCharges, command);

        var sellerContext = saleSellerContextResolver.resolve(pos);

        var promotionDecision = salePromotionEvaluator.evaluate(
            tenantId,
            command,
            pos,
            now,
            initialMoney.total(),
            mode
        );

        var appliedPromotion = mode == SaleEvaluationMode.FINAL
            ? salePromotionEffectApplier.apply(promotionDecision, paidLines, initialCharges, command, command.currency())
            : AppliedSalePromotionEffects.none(paidLines, initialCharges);

        var finalLines = appliedPromotion.ticketLines();
        var finalCharges = appliedPromotion.charges();
        var finalMoney = saleMoneyCalculator.compute(finalLines, finalCharges, command);

        var policyDecision = saleLimitAutonomyEvaluator.evaluate(
            tenantId,
            command,
            pos,
            draw,
            new SalePolicyInput(mergedLines, finalLines, finalMoney),
            now
        );

        var notices = SalesNoticeAssembler.assemble(
            policyDecision,
            finalCharges,
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
            finalLines,
            finalCharges,
            finalMoney,
            policyDecision.limits(),
            policyDecision.autonomy(),
            policyDecision.requiresApproval(),
            policyDecision.approvalLevel(),
            approvalRequestId,
            promotionDecision,
            List.copyOf(notices),
            sellerContext.sellerId(),
            sellerContext.sellerAssignmentId()
        );
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
