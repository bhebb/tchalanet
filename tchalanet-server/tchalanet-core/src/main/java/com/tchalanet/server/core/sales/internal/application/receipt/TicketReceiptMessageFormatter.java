package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptMessageFormatter {

    public TicketReceiptMessageContent format(TicketReceiptView receipt) {
        var subject = "Ticket Tchalanet " + receipt.displayCode();
        var body = """
            Ticket Tchalanet valide
            Code: %s
            Tirage: %s
            Montant: %s
            Verification: %s
            """.formatted(
            receipt.displayCode(),
            receipt.drawLabel(),
            receipt.totalAmount(),
            receipt.verificationUrl()
        );
        return new TicketReceiptMessageContent(
            subject,
            body,
            receipt.locale(),
            Map.of(
                "ticketId", receipt.ticketId().value().toString(),
                "publicCode", receipt.publicCode(),
                "displayCode", receipt.displayCode()
            )
        );
    }
}
