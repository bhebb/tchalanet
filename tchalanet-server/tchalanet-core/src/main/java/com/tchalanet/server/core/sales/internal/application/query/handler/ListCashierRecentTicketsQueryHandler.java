package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.CashierRecentTicketView;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListCashierRecentTicketsQueryHandler
    implements QueryHandler<ListCashierRecentTicketsQuery, List<CashierRecentTicketView>> {

    private final CashierTicketDashboardReaderPort reader;

    @Override
    public List<CashierRecentTicketView> handle(ListCashierRecentTicketsQuery query) {
        int limit = Math.min(Math.max(query.limit(), 1), 20);
        return reader.findRecentByCashier(query.cashierId(), limit);
    }
}
