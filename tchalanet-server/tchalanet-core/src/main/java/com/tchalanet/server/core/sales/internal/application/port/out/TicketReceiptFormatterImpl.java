package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import org.springframework.stereotype.Service;

@Service("ticketReceiptFormatterPdf")
public class TicketReceiptFormatterImpl implements TicketReceiptFormatter {
    @Override
    public String formatText(TicketPrintView t, String verifyUrl) {
        return "";
    }
}
