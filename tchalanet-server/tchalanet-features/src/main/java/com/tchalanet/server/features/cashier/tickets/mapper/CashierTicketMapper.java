package com.tchalanet.server.features.cashier.tickets.mapper;

import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketDetailsResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPageResponse;
import org.springframework.stereotype.Component;

@Component
public class CashierTicketMapper {

    public CashierTicketPageResponse toPageResponse(TicketRow row) {
        return new CashierTicketPageResponse(
            row.id(),
            row.ticketCode(),
            row.status(),
            row.drawId(),
            row.totalAmountCents(),
            row.currency(),
            row.placedAt());
    }

    public CashierTicketDetailsResponse toDetailsResponse(TicketDetailsView view) {
        return new CashierTicketDetailsResponse(
            view.id(),
            view.ticketCode(),
            view.status(),
            view.drawId(),
            view.totalAmountCents(),
            view.currency(),
            view.placedAt(),
            view.cancelledAt());
    }
}
