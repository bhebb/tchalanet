package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketSalesSummaryReaderPort;
import com.tchalanet.server.core.sales.api.query.GetTicketSalesSummaryBySessionQuery;
import com.tchalanet.server.core.sales.api.query.TicketSalesSessionSummary;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketSalesSummaryBySessionQueryHandler
    implements QueryHandler<GetTicketSalesSummaryBySessionQuery, TicketSalesSessionSummary> {

    private final TicketSalesSummaryReaderPort reader;

    @Override
    public TicketSalesSessionSummary handle(GetTicketSalesSummaryBySessionQuery query) {
        return reader.getBySession(query.tenantId(), query.sessionId());
    }
}
