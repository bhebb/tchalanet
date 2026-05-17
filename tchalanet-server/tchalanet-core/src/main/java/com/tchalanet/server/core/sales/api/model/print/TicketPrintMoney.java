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
}
