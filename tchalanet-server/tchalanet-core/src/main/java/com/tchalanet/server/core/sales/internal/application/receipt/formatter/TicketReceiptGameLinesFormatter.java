package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketReceiptGameLinesFormatter {

  private final ReceiptTextLayout layout;
  private final TicketReceiptMoneyFormatter moneyFormatter;
  private final TicketReceiptLabelResolver labelResolver;

  public List<TicketReceiptTextLine> format(
      List<TicketReceiptLineView> receiptLines,
      TicketReceiptTranslations translations,
      TicketReceiptLayoutProfile profile) {
    var safeReceiptLines = receiptLines == null ? List.<TicketReceiptLineView>of() : receiptLines;
    var lines = new ArrayList<TicketReceiptTextLine>();
    var complimentaryMaryajOnly =
        !safeReceiptLines.isEmpty()
            && safeReceiptLines.stream().allMatch(this::isComplimentaryMaryaj);
    var cols = computeColumnWidths(safeReceiptLines, translations, profile);
    if (!complimentaryMaryajOnly) {
      add(lines, header(translations, profile, cols), false);
    }
    var hasComplimentaryMaryaj = false;
    for (var line : safeReceiptLines) {
      add(lines, lineRow(line, translations, profile, cols), false);
      if (line.promotional() && !isComplimentaryMaryaj(line)) {
        var promo =
            translations.text(TicketReceiptI18nKeys.PROMOTION)
                + ": "
                + promotionLabel(line, translations);
        add(lines, layout.truncate(promo, profile.charsPerLine()), false);
      }
      hasComplimentaryMaryaj = hasComplimentaryMaryaj || isComplimentaryMaryaj(line);
    }
    if (hasComplimentaryMaryaj && !complimentaryMaryajOnly) {
      add(
          lines,
          layout.truncate(
              translations.text(TicketReceiptI18nKeys.PROMOTION_MARYAJ_OFFERED_NOTE),
              profile.charsPerLine()),
          false);
    }
    return List.copyOf(lines);
  }

  // Backwards-compatible overload used by callers that haven't migrated yet.
  public List<TicketReceiptTextLine> format(
      List<TicketReceiptLineView> receiptLines, TicketReceiptTranslations translations) {
    var profile = new TicketReceiptLayoutProfile(80, false, false, true);
    return format(receiptLines, translations, profile);
  }

  private String header(
      TicketReceiptTranslations translations, TicketReceiptLayoutProfile profile, int[] cols) {
    // Realized gains are shown after settlement through verification,
    // not as sale-time payout estimates.
    int choiceW = cols[0];
    int stakeW = cols[1];

    var partChoice =
        layout.rightPad(translations.text(TicketReceiptI18nKeys.LINE_HEADER_NO), choiceW);
    var partStake =
        layout.leftPad(translations.text(TicketReceiptI18nKeys.LINE_HEADER_STAKE), stakeW);

    return layout.truncate(partChoice + " " + partStake, profile.charsPerLine());
  }

  private String lineRow(
      TicketReceiptLineView line,
      TicketReceiptTranslations translations,
      TicketReceiptLayoutProfile profile,
      int[] cols) {
    int choiceW = cols[0];
    int stakeW = cols[1];

    var choice = choiceDisplay(line);
    var stake = stakeDisplay(line, translations, profile);
    var choicePart = layout.rightPad(choice, choiceW);
    var stakePart = layout.leftPad(stake, stakeW);

    var row = choicePart + " " + stakePart;
    return layout.truncate(row, profile.charsPerLine());
  }

  // labelResolver handles optionLabel vs betType via translations; no local fallback needed here

  private String promotionLabel(
      TicketReceiptLineView line, TicketReceiptTranslations translations) {
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

  private String selectionDisplay(TicketReceiptLineView line) {
    var selection = line.selection() == null ? "" : line.selection();
    if ("MARRIAGE_2D2D".equals(line.betType())) {
      selection = maryajSelection(selection);
    }
    if (isComplimentaryMaryaj(line)) {
      return "* " + selection;
    }
    return selection;
  }

  private String choiceDisplay(TicketReceiptLineView line) {
    var choice = selectionDisplay(line);
    var optionLabel = labelResolver.lineOptionLabel(line, null);
    if (optionLabel != null && !optionLabel.isBlank()) {
      choice = choice + "  " + optionLabel;
    }
    return choice;
  }

  private String maryajSelection(String selection) {
    if (selection == null || selection.isBlank()) {
      return "";
    }
    var normalized =
        selection
            .trim()
            .replace(" x ", " × ")
            .replace(" X ", " × ")
            .replace(" - ", " × ")
            .replace("-", " × ");
    if (normalized.matches("\\d{4}")) {
      return normalized.substring(0, 2) + " × " + normalized.substring(2);
    }
    return normalized;
  }

  private String stakeDisplay(
      TicketReceiptLineView line,
      TicketReceiptTranslations translations,
      TicketReceiptLayoutProfile profile) {
    if (isComplimentaryMaryaj(line)) {
      return translations.text(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_SHORT);
    }
    return moneyFormatter.format(line.stake(), profile);
  }

  private boolean isComplimentaryMaryaj(TicketReceiptLineView line) {
    return "HT_MARYAJ_GRATIS".equals(line.gameCode())
        && "MARRIAGE_2D2D".equals(line.betType())
        && "FREE_GAME_LINE".equals(line.promotionEffectType())
        && line.stake() != null
        && line.stake().amount().compareTo(BigDecimal.ZERO) == 0;
  }

  private int[] computeColumnWidths(
      List<TicketReceiptLineView> lines,
      TicketReceiptTranslations translations,
      TicketReceiptLayoutProfile profile) {
    int chars = profile.charsPerLine();

    int stakeW = translations.text(TicketReceiptI18nKeys.LINE_HEADER_STAKE).length();
    int choiceW = translations.text(TicketReceiptI18nKeys.LINE_HEADER_NO).length();
    for (var line : lines) {
      stakeW = Math.max(stakeW, stakeDisplay(line, translations, profile).length());
      choiceW = Math.max(choiceW, choiceDisplay(line).length());
    }

    int maxStakeW = Math.max(6, Math.min(10, chars / 2));
    stakeW = Math.min(stakeW, maxStakeW);
    int maxChoiceW = Math.max(1, chars - stakeW - 1);
    choiceW = Math.min(choiceW, maxChoiceW);

    if (choiceW + stakeW + 1 > chars) {
      choiceW = Math.max(1, chars - stakeW - 1);
    }

    return new int[] {choiceW, stakeW};
  }

  private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
    if (value != null && !value.isBlank()) {
      lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
    }
  }
}
