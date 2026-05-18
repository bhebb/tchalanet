package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.query.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.features.cashier.tickets.mapper.CashierTicketMapper;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketDetailsResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierTicketsPageService {

    private final QueryBus queryBus;
    private final CashierTicketMapper mapper;

    public TchPage<CashierTicketPageResponse> listTickets(Pageable pageable) {
        var result = queryBus.ask(new ListTicketsQuery(
            null, null, null, null, null, null, new TchPageRequest(pageable)));
        return TchPageMapper.map(result, mapper::toPageResponse);
    }

    public CashierTicketDetailsResponse getDetails(TicketId ticketId) {
        var view = queryBus.ask(new GetTicketDetailsQuery(ticketId));
        return mapper.toDetailsResponse(view);
    }
}
