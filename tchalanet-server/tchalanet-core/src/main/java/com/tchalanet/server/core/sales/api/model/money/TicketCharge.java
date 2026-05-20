package com.tchalanet.server.core.sales.api.model.money;

import com.tchalanet.server.common.types.money.Money;

import java.util.Objects;

/**
 * A single charge applied to a ticket sale, on top of the stake.
 *
 * <p>Examples: SMS notification fee, WhatsApp notification fee, optional
 * insurance, weekend surcharge. Each charge declares its type and who pays it.
 *
 * <p>Only charges where {@code paidBy == BUYER} contribute to the ticket's
 * {@code total}. Charges paid by the seller or the tenant are tracked for
 * internal cost accounting (via a separate ledger flow) but do NOT inflate
 * the buyer-facing total.
 */
public record TicketCharge(
    TicketChargeType type,
    Money amount,
    ChargePaidBy paidBy
) {
    public TicketCharge {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(paidBy, "paidBy is required");
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException("Charge amount must not be negative");
        }
    }

    public boolean isBuyerFacing() {
        return paidBy == ChargePaidBy.BUYER;
    }
}
