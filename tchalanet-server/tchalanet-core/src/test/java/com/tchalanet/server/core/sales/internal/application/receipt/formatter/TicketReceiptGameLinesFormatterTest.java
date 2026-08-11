package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TicketReceiptGameLinesFormatterTest {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final TicketReceiptLayoutProfile THERMAL_58 =
      new TicketReceiptLayoutProfile(32, true, true, false);

  private final TicketReceiptGameLinesFormatter formatter =
      new TicketReceiptGameLinesFormatter(
          new ReceiptTextLayout(),
          new TicketReceiptMoneyFormatter(),
          new TicketReceiptLabelResolver());

  @Test
  void omitsLineNumbersPotentialGainsAndImplicitOptions() {
    var lines =
        formatter.format(
            List.of(
                new TicketReceiptLineView(
                    7,
                    "HT_LOTO3",
                    "LOTTO3_3D",
                    (short) 3,
                    "Exact + Permuté",
                    "LOTO 3 CHIFFRES",
                    "123",
                    money("10"),
                    SelectionPolicy.IMPLICIT_BEST_MATCH,
                    false,
                    null,
                    null)),
            translations(),
            THERMAL_58);

    var text = joined(lines);
    assertThat(text).contains("123");
    assertThat(text).contains("10.00");
    assertThat(text).doesNotContain("#7");
    assertThat(text).doesNotContain("Gain");
    assertThat(text).doesNotContain("Exact + Permuté");
  }

  @Test
  void keepsThermalColumnsCompactForShortSelections() {
    var lines =
        formatter.format(
            List.of(
                new TicketReceiptLineView(
                    1,
                    "HT_BOLET",
                    "MATCH_1_2D",
                    null,
                    null,
                    "BORLETTE",
                    "10",
                    money("10"),
                    SelectionPolicy.IMPLICIT_BEST_MATCH,
                    false,
                    null,
                    null)),
            translations(),
            THERMAL_58);

    assertThat(lines).extracting(line -> line.text()).contains("No  Mise", "10 10.00");
    assertThat(lines).extracting(line -> line.text()).doesNotContain("No", "Mise");
  }

  @Test
  void printsExplicitOptionAndComplimentaryMaryajWithSingleNote() {
    var lines =
        formatter.format(
            List.of(
                new TicketReceiptLineView(
                    1,
                    "HT_MARYAJ",
                    "MARRIAGE_2D2D",
                    (short) 1,
                    "Permuté",
                    "MARYAJ",
                    "1234",
                    money("5"),
                    SelectionPolicy.EXPLICIT_ONLY,
                    false,
                    null,
                    null),
                new TicketReceiptLineView(
                    2,
                    "HT_MARYAJ_GRATIS",
                    "MARRIAGE_2D2D",
                    null,
                    null,
                    "MARYAJ",
                    "56-78",
                    money("0"),
                    SelectionPolicy.IMPLICIT_BEST_MATCH,
                    true,
                    "receipt.promotion.free_game_line",
                    "FREE_GAME_LINE")),
            translations(),
            THERMAL_58);

    var text = joined(lines);
    assertThat(text).contains("12 × 34");
    assertThat(text).contains("Box");
    assertThat(text).contains("12 × 34  Box");
    assertThat(text).containsPattern("12 × 34  Box\\s{2,}5\\.00");
    assertThat(text).contains("* 56 × 78");
    assertThat(text).contains("GRATIS");
    assertThat(text).contains("* Maryaj offert");
    assertThat(text).doesNotContain("Promotion: Maryaj gratuit");
  }

  @Test
  void doesNotTreatPaidMaryajZeroStakeAsComplimentaryMaryaj() {
    var lines =
        formatter.format(
            List.of(
                new TicketReceiptLineView(
                    1,
                    "HT_MARYAJ",
                    "MARRIAGE_2D2D",
                    (short) 1,
                    "Permuté",
                    "MARYAJ",
                    "1234",
                    money("0"),
                    SelectionPolicy.EXPLICIT_ONLY,
                    false,
                    null,
                    null)),
            translations(),
            THERMAL_58);

    var text = joined(lines);
    assertThat(text).contains("12 × 34");
    assertThat(text).contains("Box");
    assertThat(text).doesNotContain("* 12 × 34");
    assertThat(text).doesNotContain("* Maryaj offert");
  }

  @Test
  void compactsLongExplicitOptionLabelsOnPrintedTicket() {
    var lines =
        formatter.format(
            List.of(
                new TicketReceiptLineView(
                    1,
                    "HT_LOTO5",
                    "LOTTO5_PATTERN",
                    (short) 3,
                    "Mixte 1er/2e/3e lot",
                    "LOTO 5 CHIFFRES",
                    "22125",
                    money("1"),
                    SelectionPolicy.EXPLICIT_ONLY,
                    false,
                    null,
                    null)),
            translations(),
            THERMAL_58);

    var text = joined(lines);
    assertThat(text).contains("Mixte 1-2-3");
    assertThat(text).doesNotContain("Mixte 1er/2e/3e lot");
  }

  @Test
  void complimentaryMaryajDoesNotPrintFixedAmountsOrOdds() {
    var lines =
        formatter.format(
            List.of(
                new TicketReceiptLineView(
                    2,
                    "HT_MARYAJ_GRATIS",
                    "MARRIAGE_2D2D",
                    (short) 2,
                    "Exact",
                    "MARYAJ",
                    "56-78",
                    money("0"),
                    SelectionPolicy.EXPLICIT_ONLY,
                    true,
                    "receipt.promotion.free_game_line",
                    "FREE_GAME_LINE")),
            translations(),
            THERMAL_58);

    var text = joined(lines);
    assertThat(text).contains("* 56 × 78");
    assertThat(text).contains("GRATIS");
    assertThat(text).doesNotContain("No");
    assertThat(text).doesNotContain("Mise");
    assertThat(text).doesNotContain("* Maryaj offert");
    assertThat(text).doesNotContain("2000");
    assertThat(text).doesNotContain("10000");
    assertThat(text).doesNotContain("50");
  }

  private TicketReceiptI18nResolver.TicketReceiptTranslations translations() {
    return new TicketReceiptI18nResolver.TicketReceiptTranslations(
        Map.of(
            TicketReceiptI18nKeys.LINE_HEADER_NO, "No",
            TicketReceiptI18nKeys.LINE_HEADER_STAKE, "Mise",
            TicketReceiptI18nKeys.PROMOTION, "Promotion",
            TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE, "Maryaj gratuit",
            TicketReceiptI18nKeys.PROMOTION_FREE_GAME_SHORT, "GRATIS",
            TicketReceiptI18nKeys.PROMOTION_MARYAJ_OFFERED_NOTE, "* Maryaj offert"));
  }

  private String joined(
      List<com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine> lines) {
    return lines.stream()
        .map(com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine::text)
        .reduce("", (left, right) -> left + "\n" + right);
  }

  private Money money(String amount) {
    return new Money(new BigDecimal(amount), HTG);
  }
}
