package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.ListRecentTicketsForCashierQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListRecentTicketsForCashierHandler implements QueryHandler<ListRecentTicketsForCashierQuery, List<Ticket>> {

    private final TicketReaderPort ticketReader;

    @Override
    public List<Ticket> handle(ListRecentTicketsForCashierQuery query) {
        return ticketReader.listRecentForCashier(query.cashierId(), query.limit());
    }
}
