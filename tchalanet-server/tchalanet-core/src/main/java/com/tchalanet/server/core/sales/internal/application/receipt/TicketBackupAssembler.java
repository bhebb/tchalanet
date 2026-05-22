package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;
import org.springframework.stereotype.Component;

@Component
public class TicketBackupAssembler {

    private final TicketReceiptMessageFormatter messageFormatter;

    public TicketBackupAssembler(TicketReceiptMessageFormatter messageFormatter) {
        this.messageFormatter = messageFormatter;
    }

    public TicketBackupInfo assemble(TicketReceiptView receipt) {
        return new TicketBackupInfo(
            receipt.displayCode(),
            receipt.verificationUrl(),
            messageFormatter.formatShareableText(receipt),
            "Votre code est " + receipt.displayCode() + ".",
            "Verifier sur " + receipt.verificationUrl()
        );
    }
}
