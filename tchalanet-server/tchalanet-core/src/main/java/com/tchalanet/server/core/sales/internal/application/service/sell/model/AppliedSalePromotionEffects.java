package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;

import java.util.List;

public record AppliedSalePromotionEffects(
    List<TicketLine> ticketLines,
    List<TicketCharge> charges
) {
    public static AppliedSalePromotionEffects none(
        List<TicketLine> ticketLines,
        List<TicketCharge> charges
    ) {
        return new AppliedSalePromotionEffects(
            List.copyOf(ticketLines),
            List.copyOf(charges)
        );
    }
}
