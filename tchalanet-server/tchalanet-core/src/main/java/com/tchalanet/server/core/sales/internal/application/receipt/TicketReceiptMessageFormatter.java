package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TicketReceiptMessageFormatter {

    private final TicketReceiptI18nResolver i18nResolver;
    private final TicketReceiptMoneyFormatter moneyFormatter;
    private final TicketReceiptLabelResolver labelResolver;

    public TicketReceiptMessageContent format(TicketReceiptView receipt) {
        var translations = i18nResolver.resolve(receipt.locale(), receipt.tenantId());
        var subject = "Ticket Tchalanet " + receipt.displayCode();
        var body = """
            %s
            %s: %s
            %s:
            %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            """.formatted(
            translations.text(TicketReceiptI18nKeys.MESSAGE_VALID_TICKET),
            translations.text(TicketReceiptI18nKeys.MESSAGE_CODE),
            receipt.displayCode(),
            translations.text(TicketReceiptI18nKeys.DRAW_SECTION),
            // draw channel preferred, fallback to slot label
            firstNonBlank(receipt.drawChannelLabel(), receipt.drawLabel()),
            translations.text(TicketReceiptI18nKeys.DRAW_TIME),
            formatInstant(receipt.drawScheduledAt(), receipt.timezone()),
            translations.text(TicketReceiptI18nKeys.MESSAGE_GAMES),
            String.join("; ", lineSummaries(receipt, translations)),
            translations.text(TicketReceiptI18nKeys.MESSAGE_AMOUNT),
            moneyFormatter.format(receipt.totalAmount(), null),
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
        var drawPrimary = firstNonBlank(receipt.drawChannelLabel(), receipt.drawLabel());
        if (drawPrimary != null && !drawPrimary.isBlank()) {
            lines.add(translations.text(TicketReceiptI18nKeys.DRAW_SECTION) + ": " + drawPrimary);
        }
        // scheduled time
        if (receipt.drawScheduledAt() != null) {
            lines.add(translations.text(TicketReceiptI18nKeys.DRAW_TIME) + ": " + formatInstant(receipt.drawScheduledAt(), receipt.timezone()));
        }
        receipt.gameSections().forEach(section ->
            section.lines().forEach(line ->
                lines.add(translations.text(TicketReceiptI18nKeys.MESSAGE_GAME) + ": " + lineLabel(section, line, translations))));
        lines.add(translations.text(TicketReceiptI18nKeys.MESSAGE_AMOUNT) + ": " + receipt.totalAmount());
        // format Money for messages (include currency by default)
        lines.set(lines.size() - 1, translations.text(TicketReceiptI18nKeys.MESSAGE_AMOUNT) + ": " + moneyFormatter.format(receipt.totalAmount(), null));
        lines.add(translations.text(TicketReceiptI18nKeys.VERIFICATION) + ": " + receipt.verificationUrl());
        return String.join(System.lineSeparator(), lines);
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String formatInstant(java.time.Instant value, ZoneId timezone) {
        if (value == null) {
            return null;
        }
        return DATE_TIME.withZone(timezone == null ? ZoneId.of("UTC") : timezone).format(value);
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private List<String> lineSummaries(TicketReceiptView receipt, TicketReceiptI18nResolver.TicketReceiptTranslations translations) {
        return receipt.gameSections().stream()
            .flatMap(section -> section.lines().stream().map(line ->
                lineLabel(section, line, translations)))
            .toList();
    }

    private String lineLabel(TicketReceiptGameSectionView section, TicketReceiptLineView line, TicketReceiptI18nResolver.TicketReceiptTranslations translations) {
        var game = labelResolver.gameTitle(section, translations);
        var option = labelResolver.lineOptionLabel(line, translations);
        return game + " - " + option + " - " + line.selection();
    }
}
