package com.tchalanet.server.core.sales.internal.application.receipt;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nGlobalKeyStatsView;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptSectionContent;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptDrawFormatter;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.ReceiptTextLayout;
import com.tchalanet.server.core.sales.api.model.receipt.ReceiptBrandingDisplayMode;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptBrandingFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptFactsFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptGameLinesFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.types.money.CurrencyCode;

class TicketReceiptPrintFormatterTest {

    private static final TenantId TENANT_ID =
        TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));

    @Test
    void includesTenantAndOutletBrandingAndCanonicalReceiptFacts() {
        var layout = new ReceiptTextLayout();
        var moneyFormatter = new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter();
        var gameLines = new TicketReceiptGameLinesFormatter(layout, moneyFormatter, new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver());
        var formatter = new TicketReceiptPrintFormatter(
            new TicketReceiptBrandingFormatter(new ReceiptTextLayout()),
            new TicketReceiptFactsFormatter(new ReceiptTextLayout()),
            new TicketReceiptDrawFormatter(new ReceiptTextLayout()),
            gameLines,
            new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver(),
            new TicketReceiptI18nResolver(new StubI18nOverridesCatalog()),
            layout,
            moneyFormatter
        );

        var profile = DocumentPrintProfile.of(DocumentFormat.PDF, PaperSize.A4);
        var content = formatter.format(receipt(), profile);
        var separator = layout.separator(com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLayoutProfile.from(profile));

        assertThat(texts(content.headerLines()).stream().map(String::trim).toList()).containsSubsequence(
            "Tenant header",
            "Outlet header",
            separator,
            "Ticket: TCK-001",
            "Public code: ABCD-EFGH",
            "Sold at: 2026-05-27 09:00",
            "Terminal: POS-1",
            "Seller: Seller One"
        );
        assertThat(content.sections()).extracting(TicketReceiptSectionContent::title)
            .containsExactly("Draw", "Bolet");
        assertThat(texts(content.totals())).containsExactly(
            separator,
            "Stake total: HTG 10.00",
            "Total: HTG 10.00",
            "Max payout: HTG 125.00"
        );
        assertThat(texts(content.footerLines())).containsSubsequence(
            "Outlet footer",
            "Tenant footer",
            "Verification: https://verify.example/t/ABCDEFGH",
            "QR: ABCD-EFGH"
        );
        assertThat(content.qr().payload()).isEqualTo("https://verify.example/t/ABCDEFGH");
    }

    @Test
    void brandingFormatterSupportsExplicitDisplayModes() {
        var formatter = new TicketReceiptBrandingFormatter(new ReceiptTextLayout());
        var receipt = receipt();

        assertThat(texts(formatter.headerLines(
            receipt,
            ReceiptBrandingDisplayMode.NAME_AND_HEADER,
            ReceiptBrandingDisplayMode.NAME_AND_HEADER)))
            .containsSubsequence("Tenant Demo", "Tenant header", "Outlet Centre", "Outlet header");

        assertThat(texts(formatter.headerLines(
            receipt,
            ReceiptBrandingDisplayMode.NAME_ONLY,
            ReceiptBrandingDisplayMode.HEADER_ONLY)))
            .containsExactly("Tenant Demo", "Outlet header");
    }

    private static List<String> texts(List<TicketReceiptTextLine> lines) {
        return lines.stream().map(TicketReceiptTextLine::text).toList();
    }

    private static TicketReceiptView receipt() {
        return new TicketReceiptView(
            TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
            TENANT_ID,
            "TCK-001",
            "ABCD-EFGH",
            "ABCDEFGH",
            "VCODE123",
            TicketSaleStatus.APPROVED,
            TicketResultStatus.PENDING,
            TicketSettlementStatus.NOT_SETTLED,
            "Tenant Demo",
            "Tenant header",
            "Outlet header",
            "Haiti Soir",
            "Haiti",
            Instant.parse("2026-05-27T20:00:00Z"),
            "Outlet Centre",
            "POS-1",
            "Seller One",
            Instant.parse("2026-05-27T09:00:00Z"),
            Locale.ENGLISH,
            ZoneId.of("UTC"),
            List.of(new TicketReceiptGameSectionView(
                "HT_BOLET",
                "Bolet",
                List.of(new TicketReceiptLineView(
                    1,
                    "HT_BOLET",
                    "BOLET",
                    (short) 2,
                    "Straight",
                    "Bolet",
                    "12-34",
                    new BigDecimal("12.5"),
                    new Money(new BigDecimal("10.00"), CurrencyCode.of("HTG")),
                    new Money(new BigDecimal("125.00"), CurrencyCode.of("HTG")),
                    false,
                    null,
                    null
                ))
            )),
            new Money(new BigDecimal("10.00"), CurrencyCode.of("HTG")),
            new Money(new BigDecimal("10.00"), CurrencyCode.of("HTG")),
            new Money(new BigDecimal("125.00"), CurrencyCode.of("HTG")),
            "Outlet footer",
            "Tenant footer",
            "https://verify.example/t/ABCDEFGH"
        );
    }

    private static final class StubI18nOverridesCatalog implements I18nOverridesCatalog {
        @Override
        public TchPage<I18nOverrideView> search(SearchI18nOverridesCriteria criteria, TchPageRequest pageRequest) {
            return TchPage.of(List.of(), 0, 20, 0, 0, true, false, false);
        }

        @Override
        public Optional<I18nOverrideView> findByKey(String locale, String i18nKey) {
            return Optional.empty();
        }

        @Override
        public Map<String, String> resolveLocale(String locale, TchRequestContext ctx) {
            return values();
        }

        @Override
        public Map<String, String> resolveLocaleForTenant(String locale, TenantId tenantId) {
            return values();
        }

        @Override
        public I18nGlobalKeyStatsView keyStats() {
            return new I18nGlobalKeyStatsView(0, 0, 0);
        }

        private Map<String, String> values() {
            return Map.ofEntries(
                Map.entry(TicketReceiptI18nKeys.TICKET, "Ticket"),
                Map.entry(TicketReceiptI18nKeys.PUBLIC_CODE, "Public code"),
                Map.entry(TicketReceiptI18nKeys.SALE_TIMESTAMP, "Sold at"),
                Map.entry(TicketReceiptI18nKeys.TERMINAL, "Terminal"),
                Map.entry(TicketReceiptI18nKeys.SELLER, "Seller"),
                Map.entry(TicketReceiptI18nKeys.DRAW_SECTION, "Draw"),
                Map.entry(TicketReceiptI18nKeys.DRAW_TIME, "Draw time"),
                Map.entry(TicketReceiptI18nKeys.LINE_HEADER_NO, "No."),
                Map.entry(TicketReceiptI18nKeys.LINE_HEADER_STAKE, "Stake"),
                Map.entry(TicketReceiptI18nKeys.LINE_HEADER_PAYOUT, "Payout"),
                Map.entry(TicketReceiptI18nKeys.TOTAL_STAKE, "Stake total"),
                Map.entry(TicketReceiptI18nKeys.TOTAL_AMOUNT, "Total"),
                Map.entry(TicketReceiptI18nKeys.TOTAL_MAX_PAYOUT, "Max payout"),
                Map.entry(TicketReceiptI18nKeys.VERIFICATION, "Verification"),
                Map.entry(TicketReceiptI18nKeys.QR, "QR"),
                Map.entry(TicketReceiptI18nKeys.CURRENCY_NOTE, "Montants en {code}")
            );
        }
    }

    @Test
    void receipt_profiles_respect_width_and_currency_note() {
        var layout = new ReceiptTextLayout();
        var moneyFormatter = new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter();
        var gameLines = new TicketReceiptGameLinesFormatter(layout, moneyFormatter, new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver());
        var formatter = new TicketReceiptPrintFormatter(
            new TicketReceiptBrandingFormatter(new ReceiptTextLayout()),
            new TicketReceiptFactsFormatter(new ReceiptTextLayout()),
            new TicketReceiptDrawFormatter(new ReceiptTextLayout()),
            gameLines,
            new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver(),
            new TicketReceiptI18nResolver(new StubI18nOverridesCatalog()),
            layout,
            moneyFormatter
        );

        var profiles = List.of(
            DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM),
            DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_80MM),
            DocumentPrintProfile.of(DocumentFormat.PDF, PaperSize.A4)
        );

        for (var profile : profiles) {
            var content = formatter.format(receipt(), profile);
            var layoutProfile = com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLayoutProfile.from(profile);

            // no line must exceed width
            for (var line : content.headerLines()) {
                assertThat(line.text().length()).isLessThanOrEqualTo(layoutProfile.charsPerLine());
            }
            for (var section : content.sections()) {
                for (var line : section.lines()) {
                    assertThat(line.text().length()).isLessThanOrEqualTo(layoutProfile.charsPerLine());
                }
            }
            for (var line : content.totals()) {
                assertThat(line.text().length()).isLessThanOrEqualTo(layoutProfile.charsPerLine());
            }
            for (var line : content.footerLines()) {
                assertThat(line.text().length()).isLessThanOrEqualTo(layoutProfile.charsPerLine());
            }

            // currency note present for receipt paper (ESC_POS)
            boolean isReceipt = profile.outputFormat() == DocumentFormat.ESC_POS;
            boolean hasCurrencyNote = content.sections().stream()
                .flatMap(s -> s.lines().stream())
                .anyMatch(l -> l.text().contains("Montants") || l.text().contains("Amounts"));

            if (isReceipt) {
                assertThat(hasCurrencyNote).isTrue();
            } else {
                assertThat(hasCurrencyNote).isFalse();
            }
        }
    }

    @Test
    void long_game_label_is_truncated_and_lines_under_width() {
        var layout = new ReceiptTextLayout();
        var moneyFormatter = new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter();
        var gameLines = new TicketReceiptGameLinesFormatter(layout, moneyFormatter, new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver());
        var formatter = new TicketReceiptPrintFormatter(
            new TicketReceiptBrandingFormatter(new ReceiptTextLayout()),
            new TicketReceiptFactsFormatter(new ReceiptTextLayout()),
            new TicketReceiptDrawFormatter(new ReceiptTextLayout()),
            gameLines,
            new com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver(),
            new TicketReceiptI18nResolver(new StubI18nOverridesCatalog()),
            layout,
            moneyFormatter
        );

        var longTitle = "This is a very long game label that should be truncated to fit on a receipt line".repeat(2);
        var r = receipt();
        var section = new TicketReceiptGameSectionView(r.gameSections().get(0).gameCode(), longTitle, r.gameSections().get(0).lines());
        var receiptLong = new TicketReceiptView(
            r.ticketId(), r.tenantId(), r.ticketCode(), r.publicCode(), r.verificationUrl(), r.displayCode(), r.saleStatus(), r.resultStatus(), r.settlementStatus(),
            r.tenantDisplayName(), r.tenantReceiptHeader(), r.outletReceiptHeader(), r.drawLabel(), r.drawChannelLabel(), r.drawScheduledAt(), r.outletName(), r.terminalCode(), r.sellerDisplayName(), r.placedAt(),
            r.locale(), r.timezone(), List.of(section), r.stakeTotal(), r.totalAmount(), r.potentialPayout(), r.outletReceiptFooter(), r.tenantReceiptFooter(), r.verificationUrl()
        );

        var profile = DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM);
        var content = formatter.format(receiptLong, profile);
        var layoutProfile = com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLayoutProfile.from(profile);

        // section title truncated
        assertThat(content.sections().get(0).title().length()).isLessThanOrEqualTo(layoutProfile.charsPerLine());
        // all lines under width
        content.sections().forEach(s -> s.lines().forEach(l -> assertThat(l.text().length()).isLessThanOrEqualTo(layoutProfile.charsPerLine())));
    }
}
