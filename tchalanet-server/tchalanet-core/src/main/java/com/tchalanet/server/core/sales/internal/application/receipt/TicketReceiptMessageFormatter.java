package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import java.util.ArrayList;
import java.util.List;
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
            Jeux: %s
            Montant: %s
            Verification: %s
            """.formatted(
            receipt.displayCode(),
            receipt.drawLabel(),
            String.join("; ", lineSummaries(receipt)),
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

    public String formatShareableText(TicketReceiptView receipt) {
        var lines = new ArrayList<String>();
        lines.add("Ticket Tchalanet valide");
        lines.add("Code: " + receipt.displayCode());
        if (receipt.drawLabel() != null && !receipt.drawLabel().isBlank()) {
            lines.add("Tirage: " + receipt.drawLabel());
        }
        receipt.gameSections().forEach(section ->
            section.lines().forEach(line ->
                lines.add("Jeu: " + lineLabel(section, line))));
        lines.add("Montant: " + receipt.totalAmount());
        lines.add("Verification: " + receipt.verificationUrl());
        return String.join(System.lineSeparator(), lines);
    }

    private List<String> lineSummaries(TicketReceiptView receipt) {
        return receipt.gameSections().stream()
            .flatMap(section -> section.lines().stream().map(line -> lineLabel(section, line)))
            .toList();
    }

    private String lineLabel(TicketReceiptGameSectionView section, TicketReceiptLineView line) {
        var game = section.gameLabel() == null || section.gameLabel().isBlank()
            ? section.gameCode()
            : section.gameLabel();
        var option = line.optionLabel() == null || line.optionLabel().isBlank()
            ? line.betType()
            : line.optionLabel();
        return game + " - " + option + " - " + line.selection();
    }
}
