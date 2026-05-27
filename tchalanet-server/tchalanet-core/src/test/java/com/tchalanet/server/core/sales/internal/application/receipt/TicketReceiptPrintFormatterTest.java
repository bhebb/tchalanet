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
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
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

class TicketReceiptPrintFormatterTest {

    private static final TenantId TENANT_ID =
        TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));

    @Test
    void includesTenantAndOutletBrandingAndCanonicalReceiptFacts() {
        var formatter = new TicketReceiptPrintFormatter(
            new TicketReceiptBrandingFormatter(),
            new TicketReceiptFactsFormatter(),
            new TicketReceiptGameLinesFormatter(),
            new TicketReceiptI18nResolver(new StubI18nOverridesCatalog())
        );

        var content = formatter.format(receipt(), PrintOutputFormat.PDF);

        assertThat(texts(content.headerLines())).containsSubsequence(
            "Tenant Demo",
            "Tenant header",
            "Outlet Centre",
            "Outlet header",
            "--------------------------------",
            "Ticket: TCK-001",
            "Public code: ABCD-EFGH",
            "Sold at: 2026-05-27 09:00",
            "Terminal: POS-1",
            "Seller: Seller One"
        );
        assertThat(content.sections()).extracting(section -> section.title())
            .containsExactly("Draw", "Bolet");
        assertThat(texts(content.totals())).containsExactly(
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
                    "HTG 10.00",
                    "HTG 125.00",
                    false,
                    null,
                    null
                ))
            )),
            "HTG 10.00",
            "HTG 10.00",
            "HTG 125.00",
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
                Map.entry(TicketReceiptI18nKeys.QR, "QR")
            );
        }
    }
}
