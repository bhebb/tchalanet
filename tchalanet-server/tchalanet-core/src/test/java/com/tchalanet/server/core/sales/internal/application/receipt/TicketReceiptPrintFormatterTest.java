package com.tchalanet.server.core.sales.internal.application.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.ReceiptTextLayout;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptBrandingFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptDrawFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptDrawIdentityFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptFactsFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptGameLinesFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketReceiptPrintFormatterTest {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");

  @Test
  void tenantHeaderPrintsAtTopAndTenantFooterPrintsBeforeQr() {
    var formatter = formatter();

    var content =
        formatter.format(
            receipt(), DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM));

    assertThat(text(content.headerLines())).contains("BANQUE DE BORLETTE TCHALANET");
    assertThat(text(content.headerLines())).contains("Terminal Centre-ville");
    assertThat(text(content.footerLines())).contains("Vérification");
    assertThat(text(content.footerLines())).contains("Conservez le ticket original");
    assertThat(text(content.footerLines())).contains("Merci et bonne chance");
    assertThat(text(content.postQrLines())).isEmpty();
  }

  @Test
  void copyMarkerDistinguishesOriginalCopyAndReprint() {
    var formatter = formatter();
    var profile = DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM);

    assertThat(text(formatter.format(receipt(TicketPrintStateStatus.NOT_PRINTED), profile).headerLines()))
        .contains("ORIGINAL");
    assertThat(text(formatter.format(receipt(TicketPrintStateStatus.PRINTED), profile).headerLines()))
        .contains("COPIE");
    assertThat(text(formatter.format(receipt(TicketPrintStateStatus.REPRINTED), profile).headerLines()))
        .contains("RÉIMPRESSION");
  }

  @Test
  void copyMarkerFallsBackForSupportedLocalesWhenCatalogHasNoReceiptKeys() {
    var formatter = formatter(emptyCatalog());
    var profile = DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM);

    assertThat(
            text(
                formatter
                    .format(receipt(TicketPrintStateStatus.REPRINTED, Locale.FRENCH), profile)
                    .headerLines()))
        .contains("Réimpression")
        .doesNotContain("receipt.copy");
    assertThat(
            text(
                formatter
                    .format(receipt(TicketPrintStateStatus.REPRINTED, Locale.ENGLISH), profile)
                    .headerLines()))
        .contains("Reprint")
        .doesNotContain("receipt.copy");
    assertThat(
            text(
                formatter
                    .format(receipt(TicketPrintStateStatus.REPRINTED, Locale.forLanguageTag("ht")), profile)
                    .headerLines()))
        .contains("Reenpresyon")
        .doesNotContain("receipt.copy");
  }

  @Test
  void printsBusinessGameTitlesAndSeparatesMaryajGratis() {
    var formatter = formatter();
    var profile = DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM);

    var content = formatter.format(receiptWithAllGameSections(), profile);
    var sectionText =
        content.sections().stream()
            .map(
                section ->
                    (section.title() == null ? "" : section.title())
                        + "\n"
                        + text(section.lines()))
            .reduce("", (left, right) -> left + "\n" + right);

    assertThat(sectionText)
        .contains("Borlette")
        .contains("Maryaj")
        .contains("Maryaj gratis")
        .contains("Loto 3 chiffres")
        .contains("Loto 4 chiffres")
        .contains("Loto 5 chiffres")
        .contains("* 20 × 85")
        .contains("GRATIS")
        .doesNotContain("Mariage")
        .doesNotContain("Maryaj gratuit")
        .doesNotContain("LOTO 3 CHIFFRES");
    assertThat(sectionText.indexOf("Maryaj gratis")).isGreaterThan(sectionText.indexOf("Maryaj"));
    assertThat(sectionText.indexOf("* 20 × 85")).isGreaterThan(sectionText.indexOf("Maryaj gratis"));
  }

  @Test
  void totalsPrintOnlyTotalAmount() {
    var formatter = formatter();
    var profile = DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM);

    var content = formatter.format(receipt(), profile);

    assertThat(text(content.totals())).contains("TOTAL: 56.00").doesNotContain("Mise:");
  }

  private TicketReceiptPrintFormatter formatter() {
    return formatter(catalog());
  }

  private TicketReceiptPrintFormatter formatter(I18nOverridesCatalog catalog) {
    var layout = new ReceiptTextLayout();
    var money = new TicketReceiptMoneyFormatter();
    return new TicketReceiptPrintFormatter(
        new TicketReceiptBrandingFormatter(layout),
        new TicketReceiptFactsFormatter(layout),
        new TicketReceiptDrawFormatter(layout, new TicketReceiptDrawIdentityFormatter()),
        new TicketReceiptGameLinesFormatter(layout, money, new TicketReceiptLabelResolver()),
        new TicketReceiptLabelResolver(),
        new TicketReceiptI18nResolver(catalog),
        layout,
        money);
  }

  private I18nOverridesCatalog catalog() {
    var catalog = mock(I18nOverridesCatalog.class);
    when(catalog.resolveLocaleForTenant("fr", receiptTenant())).thenReturn(translations());
    when(catalog.resolveLocale("fr")).thenReturn(translations());
    return catalog;
  }

  private I18nOverridesCatalog emptyCatalog() {
    var catalog = mock(I18nOverridesCatalog.class);
    when(catalog.resolveLocaleForTenant("fr", receiptTenant())).thenReturn(Map.of());
    when(catalog.resolveLocaleForTenant("en", receiptTenant())).thenReturn(Map.of());
    when(catalog.resolveLocaleForTenant("ht", receiptTenant())).thenReturn(Map.of());
    when(catalog.resolveLocale("fr")).thenReturn(Map.of());
    when(catalog.resolveLocale("en")).thenReturn(Map.of());
    when(catalog.resolveLocale("ht")).thenReturn(Map.of());
    return catalog;
  }

  private Map<String, String> translations() {
    return Map.ofEntries(
        Map.entry(TicketReceiptI18nKeys.COPY_ORIGINAL, "ORIGINAL"),
        Map.entry(TicketReceiptI18nKeys.COPY_DUPLICATE, "COPIE"),
        Map.entry(TicketReceiptI18nKeys.COPY_REPRINT, "RÉIMPRESSION"),
        Map.entry(TicketReceiptI18nKeys.TICKET, "Ticket"),
        Map.entry(TicketReceiptI18nKeys.PUBLIC_CODE, "Ticket"),
        Map.entry(TicketReceiptI18nKeys.SALE_TIMESTAMP, "Vente"),
        Map.entry(TicketReceiptI18nKeys.TERMINAL, "Terminal"),
        Map.entry(TicketReceiptI18nKeys.SELLER, "Vendeur"),
        Map.entry(TicketReceiptI18nKeys.REF, "Réf."),
        Map.entry(TicketReceiptI18nKeys.DRAW_SECTION, "TIRAGE"),
        Map.entry(TicketReceiptI18nKeys.DRAW_TIME, "Heure"),
        Map.entry(TicketReceiptI18nKeys.LINE_HEADER_NO, "No"),
        Map.entry(TicketReceiptI18nKeys.LINE_HEADER_STAKE, "Mise"),
        Map.entry(TicketReceiptI18nKeys.TOTAL_STAKE, "Mise"),
        Map.entry(TicketReceiptI18nKeys.TOTAL_AMOUNT, "TOTAL"),
        Map.entry(TicketReceiptI18nKeys.VERIFICATION, "Vérification"),
        Map.entry(TicketReceiptI18nKeys.SCAN_TO_VERIFY, "Scannez le code QR"),
        Map.entry(TicketReceiptI18nKeys.PROMOTION, "Promotion"),
        Map.entry(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE, "Maryaj gratis"),
        Map.entry(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_SHORT, "GRATIS"),
        Map.entry(TicketReceiptI18nKeys.PROMOTION_MARYAJ_OFFERED_NOTE, "* Maryaj offert"),
        Map.entry(TicketReceiptI18nKeys.CURRENCY_NOTE, "MONTANTS EN {code}"));
  }

  private TicketReceiptView receipt() {
    return receipt(TicketPrintStateStatus.NOT_PRINTED);
  }

  private TicketReceiptView receipt(TicketPrintStateStatus printStateStatus) {
    return receipt(printStateStatus, Locale.FRENCH);
  }

  private TicketReceiptView receipt(TicketPrintStateStatus printStateStatus, Locale locale) {
    return receipt(printStateStatus, locale, List.of(), money("56"), money("56"));
  }

  private TicketReceiptView receiptWithAllGameSections() {
    return receipt(
        TicketPrintStateStatus.NOT_PRINTED,
        Locale.FRENCH,
        List.of(
            section("HT_BOLET", paidLine(1, "HT_BOLET", "MATCH_1_2D", "17", "1")),
            section("HT_MARYAJ", paidLine(2, "HT_MARYAJ", "MARRIAGE_2D2D", "1221", "2")),
            section(
                "HT_MARYAJ_GRATIS",
                new TicketReceiptLineView(
                    3,
                    "HT_MARYAJ_GRATIS",
                    "MARRIAGE_2D2D",
                    null,
                    null,
                    "Maryaj gratis",
                    "20-85",
                    money("0"),
                    SelectionPolicy.IMPLICIT_BEST_MATCH,
                    true,
                    "receipt.promotion.free_game_line",
                    "FREE_GAME_LINE")),
            section("HT_LOTO3", paidLine(4, "HT_LOTO3", "LOTTO3_3D", "123", "1")),
            section("HT_LOTO4", paidLine(5, "HT_LOTO4", "LOTTO4_4D", "1234", "1")),
            section("HT_LOTO5", paidLine(6, "HT_LOTO5", "LOTTO5_5D", "12345", "1"))),
        money("6"),
        money("6"));
  }

  private TicketReceiptView receipt(
      TicketPrintStateStatus printStateStatus,
      Locale locale,
      List<TicketReceiptGameSectionView> gameSections,
      Money stakeTotal,
      Money totalAmount) {
    return new TicketReceiptView(
        TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
        receiptTenant(),
        "TCK-0001",
        "HE1E-EQYJ",
        "HE1E-EQYJ",
        "111222",
        TicketSaleStatus.APPROVED,
        TicketResultStatus.NOT_RESULTED,
        TicketSettlementStatus.NOT_SETTLED,
        "BANQUE DE BORLETTE TCHALANET",
        "Terminal Centre-ville\nPort-au-Prince, Haïti",
        "GA",
        "EVE",
        "GA",
        "America/Port-au-Prince",
        "Soir",
        "Géorgie",
        Instant.parse("2026-07-09T22:59:00Z"),
        "TCH-G2PYN7",
        "Seller",
        Instant.parse("2026-07-09T21:22:00Z"),
        locale,
        ZoneId.of("America/Port-au-Prince"),
        gameSections,
        stakeTotal,
        totalAmount,
        "Conservez le ticket original.\nMerci et bonne chance !",
        "https://tickets.test/HE1E-EQYJ",
        printStateStatus,
        printStateStatus != TicketPrintStateStatus.NOT_PRINTED);
  }

  private TicketReceiptGameSectionView section(String gameCode, TicketReceiptLineView line) {
    return new TicketReceiptGameSectionView(gameCode, gameCode, List.of(line));
  }

  private TicketReceiptLineView paidLine(
      int lineNo, String gameCode, String betType, String selection, String stake) {
    return new TicketReceiptLineView(
        lineNo,
        gameCode,
        betType,
        null,
        null,
        gameCode,
        selection,
        money(stake),
        SelectionPolicy.IMPLICIT_BEST_MATCH,
        false,
        null,
        null);
  }

  private TenantId receiptTenant() {
    return TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  }

  private String text(
      List<com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine> lines) {
    return lines.stream()
        .map(com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine::text)
        .reduce("", (left, right) -> left + "\n" + right);
  }

  private Money money(String amount) {
    return new Money(new BigDecimal(amount), HTG);
  }
}
