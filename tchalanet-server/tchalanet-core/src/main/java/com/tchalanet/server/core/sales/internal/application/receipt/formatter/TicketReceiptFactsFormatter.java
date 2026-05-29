package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptSectionContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptFactsFormatter {

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<TicketReceiptTextLine> ticketIdentityLines(
        TicketReceiptView receipt,
        TicketReceiptTranslations translations
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        addLabel(lines, translations.text(TicketReceiptI18nKeys.TICKET), receipt.ticketCode(), true);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.PUBLIC_CODE), receipt.displayCode(), true);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.SALE_TIMESTAMP), formatInstant(receipt.placedAt(), receipt.timezone()), false);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.TERMINAL), receipt.terminalCode(), false);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.SELLER), receipt.sellerDisplayName(), false);
        return List.copyOf(lines);
    }

    public TicketReceiptSectionContent drawSection(
        TicketReceiptView receipt,
        TicketReceiptTranslations translations
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        add(lines, receipt.drawLabel(), true);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.DRAW_TIME), formatInstant(receipt.drawScheduledAt(), receipt.timezone()), false);
        return new TicketReceiptSectionContent(translations.text(TicketReceiptI18nKeys.DRAW_SECTION), lines);
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }

    private void addLabel(List<TicketReceiptTextLine> lines, String label, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            add(lines, label + ": " + value, bold);
        }
    }

    private String formatInstant(java.time.Instant value, ZoneId timezone) {
        if (value == null) {
            return null;
        }
        return DATE_TIME.withZone(timezone == null ? ZoneId.of("UTC") : timezone).format(value);
    }
}
