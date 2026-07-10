package com.tchalanet.server.core.sales.internal.application.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
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
    void tenantHeaderPrintsAtTopAndTenantFooterPrintsAfterQr() {
        var formatter = formatter();

        var content = formatter.format(
            receipt(),
            DocumentPrintProfile.of(DocumentFormat.ESC_POS, PaperSize.RECEIPT_58MM)
        );

        assertThat(text(content.headerLines())).contains("BANQUE DE BORLETTE TCHALANET");
        assertThat(text(content.headerLines())).contains("Terminal Centre-ville");
        assertThat(text(content.footerLines())).contains("Vérification");
        assertThat(text(content.footerLines())).doesNotContain("Conservez le ticket original");
        assertThat(text(content.postQrLines())).contains("Conservez le ticket original");
        assertThat(text(content.postQrLines())).contains("Merci et bonne chance");
    }

    private TicketReceiptPrintFormatter formatter() {
        var layout = new ReceiptTextLayout();
        var money = new TicketReceiptMoneyFormatter();
        return new TicketReceiptPrintFormatter(
            new TicketReceiptBrandingFormatter(layout),
            new TicketReceiptFactsFormatter(layout),
            new TicketReceiptDrawFormatter(layout, new TicketReceiptDrawIdentityFormatter()),
            new TicketReceiptGameLinesFormatter(layout, money, new TicketReceiptLabelResolver()),
            new TicketReceiptLabelResolver(),
            new TicketReceiptI18nResolver(catalog()),
            layout,
            money
        );
    }

    private I18nOverridesCatalog catalog() {
        var catalog = mock(I18nOverridesCatalog.class);
        when(catalog.resolveLocaleForTenant("fr", receiptTenant())).thenReturn(translations());
        when(catalog.resolveLocale("fr")).thenReturn(translations());
        return catalog;
    }

    private Map<String, String> translations() {
        return Map.ofEntries(
            Map.entry(TicketReceiptI18nKeys.COPY_ORIGINAL, "ORIGINAL"),
            Map.entry(TicketReceiptI18nKeys.COPY_DUPLICATE, "DUPLICATA"),
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
            Map.entry(TicketReceiptI18nKeys.CURRENCY_NOTE, "MONTANTS EN {code}")
        );
    }

    private TicketReceiptView receipt() {
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
            null,
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
            Locale.FRENCH,
            ZoneId.of("America/Port-au-Prince"),
            List.of(),
            money("56"),
            money("56"),
            "Conservez le ticket original.\nMerci et bonne chance !",
            "https://tickets.test/HE1E-EQYJ",
            false
        );
    }

    private TenantId receiptTenant() {
        return TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    }

    private String text(List<com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine> lines) {
        return lines.stream().map(com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine::text)
            .reduce("", (left, right) -> left + "\n" + right);
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}
