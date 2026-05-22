package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.api.query.receipt.GetTicketReceiptViewQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketReceiptAssembler;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketReceiptViewQueryHandler
    implements QueryHandler<GetTicketReceiptViewQuery, TicketReceiptView> {

    private final TicketPrintReaderPort reader;
    private final TicketReceiptAssembler assembler;

    @Override
    public TicketReceiptView handle(GetTicketReceiptViewQuery query) {
        var printView = reader.findPrintViewRequired(query.ticketId());
        return assembler.assemble(printView, null);
    }
}
