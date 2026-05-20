package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.view.TicketForPayoutView;
import com.tchalanet.server.core.sales.api.query.GetTicketForPayoutQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketForPayoutQueryHandler implements QueryHandler<GetTicketForPayoutQuery, TicketForPayoutView> {

    private final TicketProjectionReaderPort reader;

    @Override
    public TicketForPayoutView handle(GetTicketForPayoutQuery query) {
        return reader.getForPayoutById(query.ticketId());
    }
}
