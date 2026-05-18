package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.CashierTopSelectionsView;
import com.tchalanet.server.core.sales.api.query.ListCashierTopSelectionsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListCashierTopSelectionsQueryHandler
    implements QueryHandler<ListCashierTopSelectionsQuery, CashierTopSelectionsView> {

    private final CashierTicketDashboardReaderPort reader;

    @Override
    public CashierTopSelectionsView handle(ListCashierTopSelectionsQuery query) {
        int limit = Math.min(Math.max(query.limitPerDraw(), 1), 10);
        return reader.findTopSelections(query.cashierId(), query.businessDate(), limit);
    }
}
