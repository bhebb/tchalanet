package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketReceiptFactsFormatter {

    private final ReceiptTextLayout layout;

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<TicketReceiptTextLine> ticketIdentityLines(
        TicketReceiptView receipt,
        TicketReceiptTranslations translations,
        TicketReceiptLayoutProfile profile
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        addLabel(lines, translations.text(TicketReceiptI18nKeys.TICKET), receipt.ticketCode(), true, profile);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.PUBLIC_CODE), receipt.displayCode(), true, profile);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.SALE_TIMESTAMP), formatInstant(receipt.placedAt(), receipt.timezone()), false, profile);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.TERMINAL), receipt.terminalCode(), false, profile);
        addLabel(lines, translations.text(TicketReceiptI18nKeys.SELLER), receipt.sellerDisplayName(), false, profile);
        return List.copyOf(lines);
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }

    private void addLabel(List<TicketReceiptTextLine> lines, String label, String value, boolean bold, TicketReceiptLayoutProfile profile) {
        if (value != null && !value.isBlank()) {
            if (profile == null) {
                add(lines, label + ": " + value, bold);
            } else {
                add(lines, layout.labelValue(label, value, profile), bold);
            }
        }
    }

    private String formatInstant(java.time.Instant value, ZoneId timezone) {
        if (value == null) {
            return null;
        }
        return DATE_TIME.withZone(timezone == null ? ZoneId.of("UTC") : timezone).format(value);
    }
}
