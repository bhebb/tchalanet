package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketReceiptGameLinesFormatter {

    private final ReceiptTextLayout layout;
    private final TicketReceiptMoneyFormatter moneyFormatter;
    private final TicketReceiptLabelResolver labelResolver;

    public List<TicketReceiptTextLine> format(
        List<TicketReceiptLineView> receiptLines,
        TicketReceiptTranslations translations,
        TicketReceiptLayoutProfile profile
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        add(lines, header(translations, profile), false);
        for (var line : receiptLines) {
            add(lines, lineRow(line, profile), false);
            add(lines, layout.truncate(labelResolver.lineOptionLabel(line, translations), profile.charsPerLine()), true);
            if (line.promotional()) {
                var promo = translations.text(TicketReceiptI18nKeys.PROMOTION) + ": " + promotionLabel(line, translations);
                add(lines, layout.truncate(promo, profile.charsPerLine()), false);
            }
        }
        return List.copyOf(lines);
    }

    // Backwards-compatible overload used by callers that haven't migrated yet.
    public List<TicketReceiptTextLine> format(
        List<TicketReceiptLineView> receiptLines,
        TicketReceiptTranslations translations
    ) {
        var profile = new TicketReceiptLayoutProfile(80, false, false, true);
        return format(receiptLines, translations, profile);
    }

    private String header(TicketReceiptTranslations translations, TicketReceiptLayoutProfile profile) {
        // Use 3-column layout: choice ("#n selection"), stake, payout
        var cols = computeColumnWidths(profile);
        int choiceW = cols[0];
        int stakeW = cols[1];
        int payoutW = cols[2];

        var partChoice = layout.rightPad(translations.text(TicketReceiptI18nKeys.LINE_HEADER_NO), choiceW);
        var partStake = layout.leftPad(translations.text(TicketReceiptI18nKeys.LINE_HEADER_STAKE), stakeW);
        var partPayout = layout.leftPad(translations.text(TicketReceiptI18nKeys.LINE_HEADER_PAYOUT), payoutW);

        return layout.truncate(partChoice + " " + partStake + " " + partPayout, profile.charsPerLine());
    }

    private String lineRow(TicketReceiptLineView line, TicketReceiptLayoutProfile profile) {
        var cols = computeColumnWidths(profile);
        int choiceW = cols[0];
        int stakeW = cols[1];
        int payoutW = cols[2];

        var choice = "#" + line.lineNo() + " " + (line.selection() == null ? "" : line.selection());
        var choicePart = layout.rightPad(choice, choiceW);
        var stakePart = layout.leftPad(moneyFormatter.format(line.stake(), profile), stakeW);
        var payoutPart = layout.leftPad(moneyFormatter.format(line.potentialPayout(), profile), payoutW);

        var row = choicePart + " " + stakePart + " " + payoutPart;
        return layout.truncate(row, profile.charsPerLine());
    }

    // labelResolver handles optionLabel vs betType via translations; no local fallback needed here

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

    private int[] computeColumnWidths(TicketReceiptLayoutProfile profile) {
        int chars = profile.charsPerLine();

        // Heuristic: allocate fixed widths for stake/payout and rest to choice column.
        // Assumes reasonable receipt widths (>= ~15). For very small widths the truncate
        // logic will still ensure lines fit, but such profiles are out of scope for V1.
        int stakeW = 10;
        int payoutW = 10;
        int used = stakeW + payoutW + 2; // two spaces between columns
        int choiceW = chars - used;

        // ensure minimal widths
        if (choiceW < 6) {
            int deficit = 6 - choiceW;
            int reduceStake = Math.min(Math.max(0, stakeW - 6), deficit);
            stakeW -= reduceStake;
            deficit -= reduceStake;
            int reducePayout = Math.min(Math.max(0, payoutW - 6), deficit);
            payoutW -= reducePayout;
            used = stakeW + payoutW + 2;
            choiceW = chars - used;
        }

        if (choiceW < 1) {
            choiceW = 1;
        }

        return new int[]{choiceW, stakeW, payoutW};
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }
}
