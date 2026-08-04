package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.error.LimitPolicyErrorCodes;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitLineContext;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import com.tchalanet.server.core.sales.internal.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.internal.application.sale.SaleEvaluationMode;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.PreparedSale;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyDecision;
import com.tchalanet.server.core.sales.internal.application.service.sell.promotion.SalePromotionEffectApplier;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.platform.tenant.api.TenantBusinessCalendarApi;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Prepares a ticket sale: validates the command, resolves the POS frame, computes ticket lines and
 * charges, evaluates limit policy and seller autonomy, and aggregates user-facing notices.
 *
 * <p>This service makes ALL pre-commit decisions. The handler only consumes {@link PreparedSale}
 * and persists. No business logic lives in the handler.
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
  private final TenantBusinessCalendarApi tenantBusinessCalendarApi;
  private final TenantConfigApi tenantConfigApi;
  private final QueryBus queryBus;
  private final SalePromotionEffectApplier promotionEffectApplier;

  public PreparedSale prepareSale(
      SellTicketCommand command, TchRequestContext ctx, SaleEvaluationMode mode) {
    Objects.requireNonNull(command, "command is required");
    Objects.requireNonNull(ctx, "ctx is required");
    Objects.requireNonNull(mode, "mode is required");

    saleCommandValidator.validateCommand(command);

    var now = tchTimeProvider.now();
    var tenantId = ctx.effectiveTenantIdRequired();
    assertTenantActive(tenantId);
    assertTenantBusinessOpen(ctx, tenantId, now);
    saleCommandValidator.validateTenantConfiguration(command, tenantId);

    if (ctx.sellerTerminalId() != null) {
      return prepareSaleForSellerTerminal(command, ctx, now, tenantId, mode);
    }

    throw ProblemRest.of(SalesErrorCodes.SELLER_TERMINAL_REQUIRED);
  }

  private PreparedSale prepareSaleForSellerTerminal(
      SellTicketCommand command,
      TchRequestContext ctx,
      Instant now,
      TenantId tenantId,
      SaleEvaluationMode mode) {
    var draw = drawCutoffRule.requireBeforeCutoff(command.drawId(), command.drawChannelId());
    var mergedLines = command.lines();
    var paidLines =
        ticketLinePreparationService.toTicketLines(
            tenantId, ctx.sellerTerminalIdRequired(), mergedLines, command.currency());
    var charges = saleChargeCalculator.compute(tenantId, command);
    var money = saleMoneyCalculator.compute(paidLines, charges, command);
    var promotionDecision =
        evaluatePromotion(command, ctx, tenantId, now, mode, paidLines, money.total().amount());
    var promoted =
        promotionEffectApplier.apply(
            promotionDecision,
            paidLines,
            charges,
            command,
            ctx.sellerTerminalIdRequired(),
            command.currency());
    var finalMoney =
        saleMoneyCalculator.compute(promoted.ticketLines(), promoted.charges(), command);
    var policyDecision = evaluateLimits(command, ctx, tenantId, now, promoted.ticketLines());

    if (isBlockedInV0(policyDecision.limits().outcome())) {
      throw ProblemRest.of(LimitPolicyErrorCodes.LIMIT_BLOCKED);
    }

    return new PreparedSale(
        draw,
        now,
        mergedLines,
        promoted.ticketLines(),
        promoted.charges(),
        finalMoney,
        policyDecision.limits(),
        policyDecision.autonomy(),
        promotionDecision,
        SalesNoticeAssembler.assemble(policyDecision, promoted.charges(), promotionDecision));
  }

  private com.tchalanet.server.core.promotion.api.model.PromotionDecision evaluatePromotion(
      SellTicketCommand command,
      TchRequestContext ctx,
      TenantId tenantId,
      Instant now,
      SaleEvaluationMode mode,
      List<TicketLine> paidLines,
      BigDecimal paidTotal) {
    return queryBus.ask(
        new EvaluatePromotionQuery(
            new PromotionEvaluationContext(
                tenantId,
                mode == SaleEvaluationMode.PREVIEW
                    ? PromotionEvaluationPhase.SALE_PREVIEW
                    : PromotionEvaluationPhase.SALE_CONFIRMATION,
                now,
                null,
                List.of(),
                null,
                List.of(),
                ctx.sellerTerminalIdRequired(),
                ctx.userId(),
                paidTotal,
                command.currency().code(),
                paidLines.stream().map(line -> line.gameCode().name()).toList(),
                false)));
  }

  private SalePolicyDecision evaluateLimits(
      SellTicketCommand command,
      TchRequestContext ctx,
      TenantId tenantId,
      Instant now,
      List<TicketLine> paidLines) {
    var limitLines =
        paidLines.stream()
            .map(
                line ->
                    new LimitLineContext(
                        line.betType(),
                        line.selection().key().value(),
                        toCents(line.stakeAmount().amount())))
            .toList();

    var limitCtx =
        new LimitContext(
            tenantId,
            null,
            ctx.sellerTerminalId(),
            command.drawId(),
            command.drawChannelId(),
            now,
            limitLines);

    var eval = queryBus.ask(new EvaluateLimitPolicyQuery(limitCtx));

    return switch (eval.outcome()) {
      case BLOCK -> SalePolicyDecision.allowed(eval); // blocked handled by caller
      case REQUIRE_APPROVAL -> SalePolicyDecision.allowed(eval);
      case WARN, ALLOW -> SalePolicyDecision.allowed(eval);
    };
  }

  private static long toCents(BigDecimal amount) {
    return amount.movePointRight(2).longValue();
  }

  static boolean isBlockedInV0(BreachOutcome outcome) {
    return outcome == BreachOutcome.BLOCK || outcome == BreachOutcome.REQUIRE_APPROVAL;
  }

  private void assertTenantActive(TenantId tenantId) {
    var tenant = tenantConfigApi.getTenantById(new GetTenantByIdRequest(tenantId));
    if (tenant.status() != TenantStatus.ACTIVE) {
      throw ProblemRest.of(SalesErrorCodes.TENANT_DISABLED);
    }
  }

  private void assertTenantBusinessOpen(TchRequestContext ctx, TenantId tenantId, Instant now) {
    var zone = ctx.tenantZoneId() == null ? ZoneId.of("UTC") : ctx.tenantZoneId();
    var businessDate = now.atZone(zone).toLocalDate();
    var day = tenantBusinessCalendarApi.resolveBusinessDay(tenantId, businessDate);
    if (day != null && !day.open()) {
      throw ProblemRest.of(SalesErrorCodes.TENANT_BUSINESS_CLOSED);
    }
  }
}
