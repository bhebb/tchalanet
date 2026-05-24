package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class SaleMoneyCalculator {

    // -------------------------------------------------------------------------
    // Money breakdown
    // -------------------------------------------------------------------------

    public TicketMoneyBreakdown compute(
        List<TicketLine> ticketLines,
        List<TicketCharge> charges,
        SellTicketCommand command
    ) {
        var zero = Money.zero(command.currency());
        var stake = ticketLines.stream()
            .map(TicketLine::stakeAmount)
            .reduce(zero, Money::plus);
        var buyerCharges = charges.stream()
            .filter(c -> c.paidBy() == ChargePaidBy.BUYER)
            .map(TicketCharge::amount)
            .reduce(zero, Money::plus);
        var total = stake.plus(buyerCharges);
        return new TicketMoneyBreakdown(stake, charges, total);
    }

}
