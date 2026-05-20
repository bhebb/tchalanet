package com.tchalanet.server.core.sales.api.event.payload;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;

import java.util.List;

/**
 * Money snapshot inside {@link com.tchalanet.server.core.sales.api.event.TicketPlacedEvent}.
 *
 * <p>Charges are fully exposed so consumers (ledger, reporting) can route each
 * type of money to the right account. Buyer-facing charges sum to
 * {@code total - stake}.
 */
public record TicketMoneyPayload(
    CurrencyCode currency,
    Money stake,
    Money total,
    Money potentialPayout,
    List<ChargeItem> charges
) {
    public TicketMoneyPayload {
        charges = (charges == null) ? List.of() : List.copyOf(charges);
    }

    public record ChargeItem(
        TicketChargeType type,
        Money amount,
        ChargePaidBy paidBy
    ) {
    }
}
