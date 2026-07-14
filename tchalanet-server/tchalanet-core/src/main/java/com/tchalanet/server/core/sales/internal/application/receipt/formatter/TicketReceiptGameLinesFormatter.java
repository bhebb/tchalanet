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
    var lines = new ArrayList<TicketReceiptTextLine>();
    add(lines, header(translations, profile), false);
    var hasComplimentaryMaryaj = false;
    for (var line : receiptLines) {
      add(lines, lineRow(line, translations, profile), false);
      if (line.promotional() && !isComplimentaryMaryaj(line)) {
        var promo =
            translations.text(TicketReceiptI18nKeys.PROMOTION)
                + ": "
                + promotionLabel(line, translations);
        add(lines, layout.truncate(promo, profile.charsPerLine()), false);
      }
      hasComplimentaryMaryaj = hasComplimentaryMaryaj || isComplimentaryMaryaj(line);
    }
    if (hasComplimentaryMaryaj) {
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
      TicketReceiptTranslations translations, TicketReceiptLayoutProfile profile) {
    // Realized gains are shown after settlement through verification,
    // not as sale-time payout estimates.
    var cols = computeColumnWidths(profile);
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
      TicketReceiptLayoutProfile profile) {
    var cols = computeColumnWidths(profile);
    int choiceW = cols[0];
    int stakeW = cols[1];

    var choice = selectionDisplay(line);
    var optionLabel = labelResolver.lineOptionLabel(line, null);
    if (optionLabel != null && !optionLabel.isBlank()) {
      choice = choice + "  " + optionLabel;
    }
    var choicePart = layout.rightPad(choice, choiceW);
    var stakePart = layout.leftPad(stakeDisplay(line, translations, profile), stakeW);

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

  private int[] computeColumnWidths(TicketReceiptLayoutProfile profile) {
    int chars = profile.charsPerLine();

    // Heuristic: allocate fixed width for stake and rest to choice column.
    // Assumes reasonable receipt widths (>= ~15). For very small widths the truncate
    // logic will still ensure lines fit, but such profiles are out of scope for V1.
    int stakeW = 10;
    int used = stakeW + 1; // one space between columns
    int choiceW = chars - used;

    // ensure minimal widths
    if (choiceW < 6) {
      int deficit = 6 - choiceW;
      int reduceStake = Math.min(Math.max(0, stakeW - 6), deficit);
      stakeW -= reduceStake;
      used = stakeW + 1;
      choiceW = chars - used;
    }

    if (choiceW < 1) {
      choiceW = 1;
    }

    return new int[] {choiceW, stakeW};
  }

  private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
    if (value != null && !value.isBlank()) {
      lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
    }
  }
}
