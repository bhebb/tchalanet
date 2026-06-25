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
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.SelectionApi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
 *   <li>Compute the potential payout = stake × odds.</li>
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

        var oddsResolution = queryBus.ask(new ResolveSellerTerminalOddsQuery(
                tenantId,
                sellerTerminalId,
                canonicalGameCode(input.gameCode()),
                input.betType().name(),
                input.betOption()));
        Objects.requireNonNull(oddsResolution, "pricing odds resolution is required");
        var odds = requireEffectiveOdds(oddsResolution.effectiveOdds())
            .setScale(4, RoundingMode.HALF_UP);

        var potential = stake.multiply(odds).setScale(2, RoundingMode.HALF_UP);

        return new TicketLine(
            TicketLineId.of(idGenerator.newUuid()),
            input.lineNumber(),
            input.gameCode(),
            input.betType(),
            selectionApi.canonicalize(input.betType(), input.betOption(), input.rawSelection()),
            new Money(stake, currency), // stakeAmount
            new Money(stake, currency), // payoutBaseAmount = stake for normal lines
            odds, // oddsSnapshot
            new Money(potential, currency), // potentialPayoutAmount
            input.betOption(),
            com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin.CUSTOMER,
            com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource.STANDARD,
            com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource.CUSTOMER_SELECTED,
            null,
            null,
            null,
            TicketLineResultStatus.PENDING,
            Money.zero(currency)
        );
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

    /** Pricing catalog uses string game codes by convention. */
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
}
