package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketReceiptMessageFormatter {

    private final TicketReceiptI18nResolver i18nResolver;

    public TicketReceiptMessageContent format(TicketReceiptView receipt) {
        var translations = i18nResolver.resolve(receipt.locale(), receipt.tenantId());
        var subject = "Ticket Tchalanet " + receipt.displayCode();
        var body = """
            %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            """.formatted(
            translations.text(TicketReceiptI18nKeys.MESSAGE_VALID_TICKET),
            translations.text(TicketReceiptI18nKeys.MESSAGE_CODE),
            receipt.displayCode(),
            translations.text(TicketReceiptI18nKeys.DRAW_SECTION),
            receipt.drawLabel(),
            translations.text(TicketReceiptI18nKeys.MESSAGE_GAMES),
            String.join("; ", lineSummaries(receipt)),
            translations.text(TicketReceiptI18nKeys.MESSAGE_AMOUNT),
            receipt.totalAmount(),
            translations.text(TicketReceiptI18nKeys.VERIFICATION),
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
        var translations = i18nResolver.resolve(receipt.locale(), receipt.tenantId());
        var lines = new ArrayList<String>();
        lines.add(translations.text(TicketReceiptI18nKeys.MESSAGE_VALID_TICKET));
        lines.add(translations.text(TicketReceiptI18nKeys.MESSAGE_CODE) + ": " + receipt.displayCode());
        if (receipt.drawLabel() != null && !receipt.drawLabel().isBlank()) {
            lines.add(translations.text(TicketReceiptI18nKeys.DRAW_SECTION) + ": " + receipt.drawLabel());
        }
        receipt.gameSections().forEach(section ->
            section.lines().forEach(line ->
                lines.add(translations.text(TicketReceiptI18nKeys.MESSAGE_GAME) + ": " + lineLabel(section, line))));
        lines.add(translations.text(TicketReceiptI18nKeys.MESSAGE_AMOUNT) + ": " + receipt.totalAmount());
        lines.add(translations.text(TicketReceiptI18nKeys.VERIFICATION) + ": " + receipt.verificationUrl());
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
