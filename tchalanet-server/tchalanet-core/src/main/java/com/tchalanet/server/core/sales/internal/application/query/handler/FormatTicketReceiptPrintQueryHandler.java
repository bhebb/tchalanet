package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptPrintQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketReceiptAssembler;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketReceiptPrintFormatter;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FormatTicketReceiptPrintQueryHandler
    implements QueryHandler<FormatTicketReceiptPrintQuery, TicketReceiptPrintContent> {

    private final TicketPrintReaderPort reader;
    private final TicketReceiptAssembler assembler;
    private final TicketReceiptPrintFormatter formatter;

    @Override
    public TicketReceiptPrintContent handle(FormatTicketReceiptPrintQuery query) {
        var printView = reader.findPrintViewRequired(query.ticketId());
        var profile = query.documentPrintProfile();
        var receipt = assembler.assemble(printView, query.locale());
        return formatter.format(receipt, profile);
    }
}
