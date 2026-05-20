package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptMessageQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketReceiptAssembler;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketReceiptMessageFormatter;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FormatTicketReceiptMessageQueryHandler
    implements QueryHandler<FormatTicketReceiptMessageQuery, TicketReceiptMessageContent> {

    private final TicketPrintReaderPort reader;
    private final TicketReceiptAssembler assembler;
    private final TicketReceiptMessageFormatter formatter;

    @Override
    public TicketReceiptMessageContent handle(FormatTicketReceiptMessageQuery query) {
        var printView = reader.findPrintViewRequired(query.ticketId());
        var receipt = assembler.assemble(printView, query.locale());
        return formatter.format(receipt);
    }
}
