package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.query.GetTicketPrintViewQuery;
import com.tchalanet.server.features.cashier.tickets.mapper.CashierTicketMapper;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPrintResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierTicketsPrintService {

    private final QueryBus queryBus;
    private final CashierTicketMapper mapper;

    public CashierTicketPrintResponse getPrintView(TicketId ticketId) {
        var view = queryBus.ask(new GetTicketPrintViewQuery(ticketId));
        return mapper.toPrintResponse(view);
    }
}
