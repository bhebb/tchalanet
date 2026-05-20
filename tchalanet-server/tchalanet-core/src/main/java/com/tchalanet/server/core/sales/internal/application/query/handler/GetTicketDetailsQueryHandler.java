package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.query.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketDetailsQueryHandler implements QueryHandler<GetTicketDetailsQuery, TicketDetailsView> {

    private final TicketProjectionReaderPort reader;

    @Override
    public TicketDetailsView handle(GetTicketDetailsQuery query) {
        var details = reader.getDetailsById(query.ticketId());
        if (details == null) {
            throw ProblemRest.notFound("ticket.not_found", query.ticketId());
        }
        return details;
    }
}
