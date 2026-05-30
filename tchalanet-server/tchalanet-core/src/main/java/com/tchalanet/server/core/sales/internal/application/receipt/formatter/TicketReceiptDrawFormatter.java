package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptSectionContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketReceiptDrawFormatter {

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReceiptTextLayout layout;

    public TicketReceiptSectionContent drawSection(
        TicketReceiptView receipt,
        TicketReceiptTranslations translations,
        TicketReceiptLayoutProfile profile
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();

        var label = firstNonBlank(receipt.drawChannelLabel(), receipt.drawLabel());

        if (label != null && !label.isBlank()) {
            add(lines, fit(label, profile), true);
        }

        var time = formatInstant(receipt.drawScheduledAt(), receipt.timezone());
        if (time != null && !time.isBlank()) {
            if (profile == null) {
                add(lines, translations.text(TicketReceiptI18nKeys.DRAW_TIME) + ": " + time, false);
            } else {
                add(lines, layout.labelValue(translations.text(TicketReceiptI18nKeys.DRAW_TIME), time, profile), false);
            }
        }

        var title = fit(translations.text(TicketReceiptI18nKeys.DRAW_SECTION), profile);
        return new TicketReceiptSectionContent(title, lines);
    }

    private String fit(String value, TicketReceiptLayoutProfile profile) {
        return profile == null ? value : layout.truncate(value, profile.charsPerLine());
    }

    private String formatInstant(Instant value, ZoneId timezone) {
        if (value == null) {
            return null;
        }
        return DATE_TIME.withZone(timezone == null ? ZoneId.of("UTC") : timezone).format(value);
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}

