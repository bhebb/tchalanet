package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTicketsQueryHandler implements QueryHandler<ListTicketsQuery, TchPage<TicketRow>> {

    private final TicketProjectionReaderPort reader;

    @Override
    public TchPage<TicketRow> handle(ListTicketsQuery query) {
        return reader.list(query);
    }
}
