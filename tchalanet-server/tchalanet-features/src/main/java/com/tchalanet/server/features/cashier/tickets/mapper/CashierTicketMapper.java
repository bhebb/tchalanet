package com.tchalanet.server.features.cashier.tickets.mapper;

import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketDetailsResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPageResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPrintResponse;
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

    public CashierTicketPrintResponse toPrintResponse(TicketPrintView view) {
        long totalCents = view.money().totalAmount().amount()
            .multiply(java.math.BigDecimal.valueOf(100)).longValue();
        return new CashierTicketPrintResponse(
            view.identity().ticketId(),
            view.identity().ticketCode(),
            view.draw().drawId(),
            view.draw().label(),
            totalCents,
            view.metadata().currency(),
            view.printState().printed(),
            view.printState().lastPrintedAt(),
            view.metadata().placedAt());
    }
}
