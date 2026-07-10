package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketReceiptDrawIdentityFormatterTest {

    private final TicketReceiptDrawIdentityFormatter formatter = new TicketReceiptDrawIdentityFormatter();

    @Test
    void prefersI18nIdentityOverrideOverLegacyChannelLabel() {
        var receipt = receipt("HT_GA_LATE", "GA_LATE", "GA", "Haïti • Georgia • Late");
        var translations = new TicketReceiptI18nResolver.TicketReceiptTranslations(Map.of(
            "receipt.draw.identity.GA_LATE", "Jòji · Ta"
        ));

        assertThat(formatter.label(receipt, translations)).isEqualTo("Jòji · Ta");
    }

    @Test
    void composesProviderAndSlotOverridesWhenFullIdentityIsMissing() {
        var receipt = receipt("HT_GA_LATE", "GA_LATE", "GA", "Haïti • Georgia • Late");
        var translations = new TicketReceiptI18nResolver.TicketReceiptTranslations(Map.of(
            "receipt.draw.provider.GA", "Georgia",
            "receipt.draw.slot.LATE", "Late"
        ));

        assertThat(formatter.label(receipt, translations)).isEqualTo("Georgia · Late");
    }

    @Test
    void fallsBackToStableCodesWhenTranslationsAreMissing() {
        var receipt = receipt("HT_GA_LATE", "GA_LATE", "GA", "Haïti • Georgia • Late");

        assertThat(formatter.label(receipt, new TicketReceiptI18nResolver.TicketReceiptTranslations(Map.of())))
            .isEqualTo("GA · LATE");
    }

    @Test
    void fallsBackToLegacyLabelWhenStructuredIdentityIsMissing() {
        var receipt = receipt(null, null, null, "Canal spécial");

        assertThat(formatter.label(receipt)).isEqualTo("Canal spécial");
    }

    private TicketReceiptView receipt(
        String drawChannelCode,
        String resultSlotKey,
        String provider,
        String legacyLabel
    ) {
        var zero = new Money(BigDecimal.ZERO, CurrencyCode.of("HTG"));
        return new TicketReceiptView(
            TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
            TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001")),
            "TCK-1",
            "ABCD-EFGH",
            "ABCDEFGH",
            "ABCDEFGH",
            TicketSaleStatus.APPROVED,
            TicketResultStatus.PENDING,
            TicketSettlementStatus.NOT_SETTLED,
            "Tchalanet",
            null,
            null,
            drawChannelCode,
            resultSlotKey,
            provider,
            "America/New_York",
            null,
            legacyLabel,
            Instant.parse("2026-07-05T02:30:00Z"),
            "TERM-1",
            "Seller",
            Instant.parse("2026-07-04T20:00:00Z"),
            Locale.FRENCH,
            ZoneId.of("America/Port-au-Prince"),
            List.of(),
            zero,
            zero,
            null,
            "https://example.test/t/ABCDEFGH",
            false
        );
    }
}
