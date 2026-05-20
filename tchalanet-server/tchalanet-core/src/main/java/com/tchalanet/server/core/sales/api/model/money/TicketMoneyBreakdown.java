package com.tchalanet.server.core.sales.api.model.money;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;

import java.util.List;
import java.util.Objects;

/**
 * Money breakdown for a ticket sale.
 *
 * <p>Invariants enforced at construction:
 * <ul>
 *   <li>{@code stake}, all charges, and {@code total} share the same currency.</li>
 *   <li>{@code total == stake + Σ buyer-facing charges}.</li>
 * </ul>
 *
 * <p>Charges paid by SELLER or TENANT are still listed (for reporting and
 * ledger), but they do NOT contribute to {@code total}. Only BUYER-facing
 * charges inflate the buyer total.
 *
 * <p>The {@code charges} list is immutable. Pass {@link List#of()} when there
 * is no charge.
 */
public record TicketMoneyBreakdown(
    Money stake,
    List<TicketCharge> charges,
    Money total
) {
    public TicketMoneyBreakdown {
        Objects.requireNonNull(stake, "stake is required");
        Objects.requireNonNull(charges, "charges is required (use List.of() if none)");
        Objects.requireNonNull(total, "total is required");

        CurrencyCode currency = stake.currency();
        if (!total.currency().equals(currency)) {
            throw new IllegalArgumentException(
                "Total currency must match stake currency");
        }
        for (var charge : charges) {
            if (!charge.amount().currency().equals(currency)) {
                throw new IllegalArgumentException(
                    "Charge currency must match stake currency: " + charge);
            }
        }

        var buyerFacing = charges.stream()
            .filter(TicketCharge::isBuyerFacing)
            .map(TicketCharge::amount)
            .reduce(Money.zero(currency), Money::plus);

        var expectedTotal = stake.plus(buyerFacing);
        if (!expectedTotal.equals(total)) {
            throw new IllegalArgumentException(
                "Invariant violated: stake (" + stake + ") + buyer-facing charges ("
                    + buyerFacing + ") must equal total (" + total + "), got " + expectedTotal);
        }

        charges = List.copyOf(charges);
    }

    /**
     * Sum of all charges regardless of who pays them. Useful for reporting.
     */
    public Money totalChargesAllPayers() {
        return charges.stream()
            .map(TicketCharge::amount)
            .reduce(Money.zero(stake.currency()), Money::plus);
    }

    /**
     * Sum of charges that are on the buyer (and thus part of the buyer total).
     */
    public Money totalBuyerCharges() {
        return charges.stream()
            .filter(TicketCharge::isBuyerFacing)
            .map(TicketCharge::amount)
            .reduce(Money.zero(stake.currency()), Money::plus);
    }

    public CurrencyCode currency() {
        return stake.currency();
    }
}
