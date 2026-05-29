package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.money.Money;
import java.util.List;

public record TicketPrintMoney(
    Money stake,
    List<TicketPrintCharge> charges,
    Money buyerChargesTotal,
    Money totalAmount,
    Money potentialPayoutAmount
) {
    public TicketPrintMoney {
        charges = List.copyOf(charges);
    }

    /**
     * Total amount saved from waived charges (promotion discount on fees).
     * Returns zero if no charges were waived.
     */
    public Money totalWaivedCharges() {
        return charges.stream()
            .filter(TicketPrintCharge::isWaived)
            .map(TicketPrintCharge::amount)
            .reduce(Money.zero(totalAmount.currency()), Money::plus);
    }

    /** True if any charge was waived by a promotion rule. */
    public boolean hasWaivedCharges() {
        return charges.stream().anyMatch(TicketPrintCharge::isWaived);
    }
}
