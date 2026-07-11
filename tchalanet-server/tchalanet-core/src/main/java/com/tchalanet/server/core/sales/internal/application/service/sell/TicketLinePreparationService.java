package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalPayoutRuleQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.settlement.PayoutRuleSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermsSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementWinMode;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.WinMode;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds {@link TicketLine} instances from validated {@link SellTicketLineInput}s.
 *
 * <p>This service trusts its caller for input validity: command-level checks
 * (line number, stake positive, bet type / option range, game support) live in
 * {@link SalePreparationOrchestrator#validateCommand}. The checks left here are
 * internal invariants that should never fire if the policy service did its job
 * — if they do, it's a bug, not a user error.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate the line id via {@link IdGenerator}.</li>
 *   <li>Resolve effective payout rules via {@link ResolveSellerTerminalPayoutRuleQuery}.</li>
 *   <li>Snapshot the settlement terms used later if the official result matches.</li>
 *   <li>Canonicalize the raw selection via {@link SelectionApi}.</li>
 *   <li>Wrap amounts in {@link Money} with the ticket's currency.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TicketLinePreparationService {

    private final SelectionApi selectionApi;
    private final IdGenerator idGenerator;
    private final QueryBus queryBus;
    private final TicketSettlementTermPlanner settlementTermPlanner;

    public List<TicketLine> toTicketLines(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        List<SellTicketLineInput> lines,
        CurrencyCode currency
    ) {
        return lines.stream()
            .map(l -> toTicketLine(tenantId, sellerTerminalId, l, currency))
            .toList();
    }

    private TicketLine toTicketLine(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        SellTicketLineInput input,
        CurrencyCode currency
    ) {
        // Internal invariants — must hold if validateCommand ran first.
        assertInternalInvariants(input);

        var stake = input.stakeAmount().setScale(2, RoundingMode.UNNECESSARY);
        var termPlan = settlementTermPlanner.plan(tenantId, input);
        var termStakes = stakeAllocation(stake, termPlan);
        var preparedTerms = new ArrayList<PreparedSettlementTerm>();
        for (int i = 0; i < termPlan.terms().size(); i++) {
            var plannedTerm = termPlan.terms().get(i);
            var termStake = termStakes.get(i);
            var resolvedPayoutRule = resolvePayoutRule(
                tenantId,
                sellerTerminalId,
                input,
                plannedTerm);
            var payoutRule = resolvedPayoutRule.payoutRule();
            preparedTerms.add(new PreparedSettlementTerm(
                plannedTerm.pricingVariantCode(),
                termStake,
                plannedTerm.winMode(),
                payoutRule,
                resolvedPayoutRule.source()));
        }
        var settlementTermsSnapshot = settlementTermsSnapshot(termPlan, preparedTerms);

        return new TicketLine(
            TicketLineId.of(idGenerator.newUuid()),
            input.lineNumber(),
            input.gameCode(),
            input.betType(),
            selectionApi.canonicalize(input.betType(), termPlan.canonicalBetOption(), input.rawSelection()),
            new Money(stake, currency), // stakeAmount
            settlementTermsSnapshot,
            input.betOption(),
            termPlan.selectionPolicySnapshot(),
            termPlan.betOptionLabelSnapshot(),
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            TicketLineSelectionSource.CUSTOMER_SELECTED,
            null,
            null,
            null,
            TicketLineResultStatus.PENDING,
            Money.zero(currency)
        );
    }

    private SettlementTermsSnapshot settlementTermsSnapshot(
        TicketSettlementTermPlan termPlan,
        List<PreparedSettlementTerm> preparedTerms
    ) {
        var policy = termPlan.selectionPolicySnapshot();
        var terms = new ArrayList<SettlementTermSnapshot>();
        for (var preparedTerm : preparedTerms) {
            var payoutRule = preparedTerm.payoutRule();
            terms.add(new SettlementTermSnapshot(
                SettlementRuleCode.fromPricingVariant(preparedTerm.pricingVariantCode()),
                policy == SelectionPolicy.IMPLICIT_BEST_MATCH ? null : termPlan.canonicalBetOption(),
                policy == SelectionPolicy.IMPLICIT_BEST_MATCH ? null : termPlan.betOptionLabelSnapshot(),
                payoutRule,
                payoutRule.type() == PayoutRuleType.STAKE_MULTIPLIER ? preparedTerm.stakeAmount() : null,
                toSettlementWinMode(preparedTerm.winMode()),
                toSettlementTermSource(preparedTerm.source())
            ));
        }
        return SettlementTermsSnapshot.current(policy, terms);
    }

    private SettlementTermSource toSettlementTermSource(OddsSource source) {
        return source == OddsSource.SELLER_TERMINAL_OVERRIDE
            ? SettlementTermSource.SELLER_TERMINAL_OVERRIDE
            : SettlementTermSource.TENANT_DEFAULT;
    }

    private SettlementWinMode toSettlementWinMode(WinMode winMode) {
        return winMode == WinMode.CUMULATIVE
            ? SettlementWinMode.CUMULATIVE
            : SettlementWinMode.ALTERNATIVE;
    }

    private ResolvedPayoutRule resolvePayoutRule(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        SellTicketLineInput input,
        PlannedSettlementTerm plannedTerm
    ) {
        var resolution = queryBus.ask(new ResolveSellerTerminalPayoutRuleQuery(
            tenantId,
            sellerTerminalId,
            canonicalGameCode(input.gameCode()),
            plannedTerm.pricingVariantCode(),
            input.betType().name(),
            plannedTerm.sourceBetOption()));
        Objects.requireNonNull(resolution, "pricing payout rule resolution is required");
        if (resolution.effectiveRuleType() == PayoutRuleType.FIXED_AMOUNT) {
            return new ResolvedPayoutRule(
                PayoutRuleSnapshot.fixedAmount(resolution.effectiveFixedAmount()),
                resolution.source());
        }
        return new ResolvedPayoutRule(
            PayoutRuleSnapshot.stakeMultiplier(requireEffectiveOdds(resolution.effectiveMultiplier())),
            resolution.source());
    }

    private static void assertInternalInvariants(SellTicketLineInput input) {
        if (input.betType() == null) {
            throw new IllegalStateException("betType is null after command validation");
        }
        if (input.stakeAmount() == null || input.stakeAmount().signum() <= 0) {
            throw new IllegalStateException("stake is non-positive after command validation");
        }
        if (input.gameCode() == null) {
            throw new IllegalStateException("gameCode is null after command validation");
        }
        // No need to re-check betOption ranges or game/betType support;
        // already enforced by TicketSalePolicyService.validateBetOption / validateLine.
    }

    /**
     * Pricing catalog uses string game codes by convention.
     */
    private static String canonicalGameCode(GameCode gameCode) {
        return gameCode.name();
    }

    private static BigDecimal requireEffectiveOdds(BigDecimal odds) {
        Objects.requireNonNull(odds, "pricing effective odds is required");
        if (odds.signum() <= 0) {
            throw new IllegalStateException("pricing effective odds must be positive");
        }
        return odds;
    }

    private static List<BigDecimal> stakeAllocation(BigDecimal stake, TicketSettlementTermPlan termPlan) {
        if (termPlan.stakeAllocationMode() == StakeAllocationMode.FULL_STAKE_PER_ALTERNATIVE) {
            return java.util.Collections.nCopies(termPlan.terms().size(), stake);
        }
        return splitStake(stake, termPlan.terms().size());
    }

    private static List<BigDecimal> splitStake(BigDecimal stake, int coverageCount) {
        if (coverageCount <= 0) {
            throw new IllegalArgumentException("coverage count must be positive");
        }
        var totalCents = stake.movePointRight(2).longValueExact();
        var baseCents = totalCents / coverageCount;
        var remainder = totalCents % coverageCount;
        var result = new ArrayList<BigDecimal>(coverageCount);
        for (int i = 0; i < coverageCount; i++) {
            var cents = baseCents + (i < remainder ? 1 : 0);
            result.add(BigDecimal.valueOf(cents, 2));
        }
        return List.copyOf(result);
    }
}

record PreparedSettlementTerm(
    PricingVariantCode pricingVariantCode,
    BigDecimal stakeAmount,
    WinMode winMode,
    PayoutRuleSnapshot payoutRule,
    OddsSource source
) {}

record ResolvedPayoutRule(
    PayoutRuleSnapshot payoutRule,
    OddsSource source
) {}
