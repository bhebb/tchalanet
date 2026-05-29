package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import java.util.List;

public record SalePolicyInput(
    List<SellTicketLineInput> paidBasis,
    List<TicketLine> finalBasis,
    TicketMoneyBreakdown finalMoney
) {
    public SalePolicyInput {
        paidBasis = paidBasis == null ? List.of() : List.copyOf(paidBasis);
        finalBasis = finalBasis == null ? List.of() : List.copyOf(finalBasis);
    }
}
