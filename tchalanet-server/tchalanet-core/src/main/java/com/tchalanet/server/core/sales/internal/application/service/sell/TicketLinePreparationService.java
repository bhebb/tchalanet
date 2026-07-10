package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalOddsQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.coverage.SettlementPayoutMode;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLineCoverage;
import com.tchalanet.server.core.selection.api.SelectionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
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
 *   <li>Resolve effective odds via {@link ResolveSellerTerminalOddsQuery}.</li>
 *   <li>Snapshot the payout rule used later if the official result matches.</li>
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
    private final TicketLineCoveragePlanner coveragePlanner;

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
        var coveragePlan = coveragePlanner.plan(tenantId, input);
        var coverageStakes = stakeAllocation(stake, coveragePlan);
        var coverages = new ArrayList<TicketLineCoverage>();
        for (int i = 0; i < coveragePlan.coverages().size(); i++) {
            var plannedCoverage = coveragePlan.coverages().get(i);
            var coverageStake = coverageStakes.get(i);
            var odds = resolveOdds(
                tenantId,
                sellerTerminalId,
                input,
                plannedCoverage)
                .setScale(4, RoundingMode.HALF_UP);
            var settlementPayout = coverageStake.multiply(odds).setScale(2, RoundingMode.HALF_UP);
            coverages.add(new TicketLineCoverage(
                plannedCoverage.pricingVariantCode(),
                new Money(coverageStake, currency),
                odds,
                new Money(settlementPayout, currency),
                plannedCoverage.winMode()
            ));
        }
        var minSettlement = coverages.stream()
            .map(TicketLineCoverage::settlementPayoutSnapshot)
            .min(Comparator.comparing(Money::amount))
            .orElseThrow();
        var maxSettlement = coverages.stream()
            .map(TicketLineCoverage::settlementPayoutSnapshot)
            .max(Comparator.comparing(Money::amount))
            .orElseThrow();
        var totalSettlement = coveragePlan.settlementPayoutMode() == SettlementPayoutMode.RANGE_CUMULATIVE
            ? coverages.stream()
                .map(TicketLineCoverage::settlementPayoutSnapshot)
                .reduce(Money.zero(currency), Money::plus)
            : null;
        var lineSettlement = totalSettlement == null ? maxSettlement : totalSettlement;
        var lineOdds = coverages.stream()
            .max(Comparator.comparing(coverage -> coverage.settlementPayoutSnapshot().amount()))
            .orElseThrow()
            .oddsSnapshot();

        return new TicketLine(
            TicketLineId.of(idGenerator.newUuid()),
            input.lineNumber(),
            input.gameCode(),
            input.betType(),
            selectionApi.canonicalize(input.betType(), coveragePlan.canonicalBetOption(), input.rawSelection()),
            new Money(stake, currency), // stakeAmount
            new Money(stake, currency), // payoutBaseAmount = stake for normal lines
            lineOdds, // oddsSnapshot: compatibility summary; coverages carry authoritative odds
            lineSettlement, // in-memory settlement summary; coverages carry authoritative rules
            coveragePlan.settlementPayoutMode(),
            minSettlement,
            maxSettlement,
            totalSettlement,
            List.copyOf(coverages),
            input.betOption(),
            coveragePlan.selectionPolicySnapshot(),
            coveragePlan.betOptionLabelSnapshot(),
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

    private BigDecimal resolveOdds(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        SellTicketLineInput input,
        PlannedTicketLineCoverage plannedCoverage
    ) {
        var oddsResolution = queryBus.ask(new ResolveSellerTerminalOddsQuery(
            tenantId,
            sellerTerminalId,
            canonicalGameCode(input.gameCode()),
            plannedCoverage.pricingVariantCode(),
            input.betType().name(),
            plannedCoverage.sourceBetOption()));
        Objects.requireNonNull(oddsResolution, "pricing odds resolution is required");
        return requireEffectiveOdds(oddsResolution.effectiveOdds());
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

    private static List<BigDecimal> stakeAllocation(BigDecimal stake, TicketLineCoveragePlan coveragePlan) {
        if (coveragePlan.stakeAllocationMode() == StakeAllocationMode.FULL_STAKE_PER_ALTERNATIVE) {
            return java.util.Collections.nCopies(coveragePlan.coverages().size(), stake);
        }
        return splitStake(stake, coveragePlan.coverages().size());
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
