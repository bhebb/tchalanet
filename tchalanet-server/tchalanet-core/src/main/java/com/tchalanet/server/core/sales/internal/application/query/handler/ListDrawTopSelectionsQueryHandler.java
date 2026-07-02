package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.DrawTopSelectionsView;
import com.tchalanet.server.core.sales.api.query.ListDrawTopSelectionsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListDrawTopSelectionsQueryHandler
    implements QueryHandler<ListDrawTopSelectionsQuery, DrawTopSelectionsView> {

    private final CashierTicketDashboardReaderPort reader;

    @Override
    public DrawTopSelectionsView handle(ListDrawTopSelectionsQuery query) {
        int limit = Math.min(Math.max(query.limit(), 1), 10);
        return reader.findTopSelectionsByDraw(query.tenantId(), query.drawId(), limit);
    }
}
