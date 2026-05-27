package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptGameLinesFormatter {

    public List<TicketReceiptTextLine> format(
        List<TicketReceiptLineView> receiptLines,
        TicketReceiptTranslations translations
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        add(lines, header(translations), false);
        for (var line : receiptLines) {
            add(lines, lineRow(line), false);
            add(lines, lineLabel(line.betType(), line.optionLabel()), true);
            if (line.promotional()) {
                add(lines, translations.text(TicketReceiptI18nKeys.PROMOTION) + ": " + promotionLabel(line, translations), false);
            }
        }
        return List.copyOf(lines);
    }

    private String header(TicketReceiptTranslations translations) {
        return "%s       %s        %s".formatted(
            translations.text(TicketReceiptI18nKeys.LINE_HEADER_NO),
            translations.text(TicketReceiptI18nKeys.LINE_HEADER_STAKE),
            translations.text(TicketReceiptI18nKeys.LINE_HEADER_PAYOUT)
        );
    }

    private String lineRow(TicketReceiptLineView line) {
        return "#%s %s %s %s".formatted(
            line.lineNo(),
            rightPad(line.selection(), 8),
            leftPad(line.stake(), 10),
            line.potentialPayout()
        );
    }

    private String lineLabel(String betType, String optionLabel) {
        return optionLabel == null || optionLabel.isBlank() ? betType : optionLabel;
    }

    private String promotionLabel(TicketReceiptLineView line, TicketReceiptTranslations translations) {
        if (line.promotionLabel() != null && !line.promotionLabel().isBlank()) {
            return line.promotionLabel().startsWith("receipt.")
                ? translations.text(line.promotionLabel())
                : line.promotionLabel();
        }
        if ("FREE_GAME_LINE".equals(line.promotionEffectType())) {
            return translations.text(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE);
        }
        if ("BOOST_ODDS".equals(line.promotionEffectType())) {
            return translations.text(TicketReceiptI18nKeys.PROMOTION_BOOST_ODDS);
        }
        return translations.text(TicketReceiptI18nKeys.PROMOTION);
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }

    private String rightPad(String value, int width) {
        var text = value == null ? "" : value;
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    private String leftPad(String value, int width) {
        var text = value == null ? "" : value;
        return text.length() >= width ? text : " ".repeat(width - text.length()) + text;
    }
}
