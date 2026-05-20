package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.query.GetTicketPrintViewQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketPrintViewQueryHandler
    implements QueryHandler<GetTicketPrintViewQuery, TicketPrintView> {

    private final TicketPrintReaderPort reader;

    @Override
    public TicketPrintView handle(GetTicketPrintViewQuery query) {
        return reader.findPrintViewRequired(query.ticketId());
    }
}
