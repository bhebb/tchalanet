package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptPrintFormatter {

    public TicketReceiptPrintContent format(TicketReceiptView receipt, PrintOutputFormat format) {
        var lines = new ArrayList<String>();
        add(lines, receipt.outletName());
        add(lines, "Ticket: " + receipt.ticketCode());
        add(lines, "Code: " + receipt.displayCode());
        add(lines, "Tirage: " + receipt.drawLabel());
        add(lines, "Vente: " + receipt.placedAt());
        add(lines, "Terminal: " + receipt.terminalCode());
        add(lines, "Vendeur: " + receipt.sellerDisplayName());
        add(lines, "--------------------------------");

        for (var section : receipt.gameSections()) {
            add(lines, section.gameLabel() == null ? section.gameCode() : section.gameLabel());
            for (var line : section.lines()) {
                add(lines, "#" + line.lineNo() + " " + line.betType() + betOptionLabel(line.betOption()));
                add(lines, "Selection: " + line.selection());
                add(lines, "Mise: " + line.stake());
                add(lines, "Gain potentiel: " + line.potentialPayout());
            }
            add(lines, "--------------------------------");
        }

        add(lines, "Mise: " + receipt.stakeTotal());
        add(lines, "TOTAL: " + receipt.totalAmount());
        add(lines, "Gain max: " + receipt.potentialPayout());
        add(lines, "Verifier: " + receipt.verificationUrl());

        return new TicketReceiptPrintContent(
            "Ticket Tchalanet",
            lines,
            receipt.verificationUrl(),
            "ticket-" + receipt.displayCode(),
            receipt.locale(),
            Map.of(
                "ticketId", receipt.ticketId().value().toString(),
                "publicCode", receipt.publicCode(),
                "displayCode", receipt.displayCode(),
                "format", format.name()
            )
        );
    }

    private void add(ArrayList<String> lines, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(value);
        }
    }

    private String betOptionLabel(Short betOption) {
        return betOption == null ? "" : " option " + betOption;
    }
}
