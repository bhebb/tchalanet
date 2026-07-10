package com.tchalanet.server.core.sales.internal.application.receipt;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.config.TicketPublicProperties;
import com.tchalanet.server.core.sales.api.model.coverage.PotentialGainMode;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintBranding;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintDraw;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintIdentity;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLifecycle;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLineCoverage;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintMetadata;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintMoney;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintQrPayload;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintSellerContext;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintState;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketVerificationUrlBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketReceiptAssemblerTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");

    @Test
    void receiptLineUsesCommercialBetOptionLabel() {
        var receipt = assembler().assemble(printViewWithLoto4BoxLine(), Locale.FRENCH);

        var line = receipt.gameSections().getFirst().lines().getFirst();
        assertThat(line.betType()).isEqualTo(BetType.LOTTO4_PATTERN.name());
        assertThat(line.betOption()).isEqualTo((short) 2);
        assertThat(line.optionLabel()).isEqualTo("Désordre / Box");
    }

    @Test
    void receiptLineCarriesCoverageSummaryWithoutTechnicalVariantLabels() {
        var receipt = assembler().assemble(printViewWithExactPlusBoxLine(), Locale.FRENCH);

        var line = receipt.gameSections().getFirst().lines().getFirst();
        assertThat(line.optionLabel()).isEqualTo("Exact + Permuté");
    }

    private TicketReceiptAssembler assembler() {
        return new TicketReceiptAssembler(
            new TicketPublicCodeFormatter(),
            new TicketVerificationUrlBuilder(new TicketPublicProperties("https://tickets.test", "/check/{code}"))
        );
    }

    private TicketPrintView printViewWithLoto4BoxLine() {
        var now = Instant.parse("2026-07-06T15:00:00Z");
        var stake = money("25");
        return new TicketPrintView(
            new TicketPrintIdentity(TicketId.of(UUID.randomUUID()), TenantId.of(UUID.randomUUID()),
                "T-0001", "abcd1234", "111222"),
            new TicketPrintLifecycle(TicketSaleStatus.APPROVED, TicketResultStatus.NOT_RESULTED,
                TicketSettlementStatus.NOT_SETTLED),
            TicketPrintState.notPrinted(),
            new TicketPrintDraw(DrawId.of(UUID.randomUUID()), DrawChannelId.of(UUID.randomUUID()),
                "NY", "MIDDAY", "provider-1", "America/Port-au-Prince", "Midi", "New York",
                LocalDate.parse("2026-07-06"), now, now.plusSeconds(3600)),
            new TicketPrintSellerContext(SellerTerminalId.of(UUID.randomUUID()), "TERM-1", "Terminal 1", "Terminal 1"),
            new TicketPrintBranding("Tenant", null, null, null, null),
            List.of(new TicketPrintLine(
                1,
                GameCode.HT_LOTO4,
                BetType.LOTTO4_PATTERN,
                (short) 2,
                "Loto 4",
                "1234",
                "1234",
                new BigDecimal("100"),
                stake,
                TicketLineOrigin.CUSTOMER,
                TicketLinePricingSource.STANDARD,
                TicketLineSelectionSource.CUSTOMER_SELECTED,
                stake,
                null,
                null,
                null
            )),
            new TicketPrintMoney(stake, List.of(), money("0"), stake),
            new TicketPrintQrPayload("v1", "ABCD1234", "111222", "https://tickets.test/check/ABCD1234", "payload"),
            new TicketPrintMetadata(now, Locale.FRENCH, ZoneId.of("America/Port-au-Prince"),
                TicketSaleChannel.POS_ONLINE, HTG.value(), Map.of())
        );
    }

    private TicketPrintView printViewWithExactPlusBoxLine() {
        var now = Instant.parse("2026-07-06T15:00:00Z");
        var stake = money("20");
        return new TicketPrintView(
            new TicketPrintIdentity(TicketId.of(UUID.randomUUID()), TenantId.of(UUID.randomUUID()),
                "T-0001", "abcd1234", "111222"),
            new TicketPrintLifecycle(TicketSaleStatus.APPROVED, TicketResultStatus.NOT_RESULTED,
                TicketSettlementStatus.NOT_SETTLED),
            TicketPrintState.notPrinted(),
            new TicketPrintDraw(DrawId.of(UUID.randomUUID()), DrawChannelId.of(UUID.randomUUID()),
                "NY", "MIDDAY", "provider-1", "America/Port-au-Prince", "Midi", "New York",
                LocalDate.parse("2026-07-06"), now, now.plusSeconds(3600)),
            new TicketPrintSellerContext(SellerTerminalId.of(UUID.randomUUID()), "TERM-1", "Terminal 1", "Terminal 1"),
            new TicketPrintBranding("Tenant", null, null, null, null),
            List.of(new TicketPrintLine(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                (short) 3,
                "Loto 3",
                "123",
                "123",
                new BigDecimal("500"),
                stake,
                List.of(
                    new TicketPrintLineCoverage(
                        "LOTTO3_STRAIGHT",
                        money("10"),
                        new BigDecimal("500"),
                        "ALTERNATIVE"),
                    new TicketPrintLineCoverage(
                        "LOTTO3_BOX_6_WAY",
                        money("10"),
                        new BigDecimal("80"),
                        "ALTERNATIVE")
                ),
                TicketLineOrigin.CUSTOMER,
                TicketLinePricingSource.STANDARD,
                TicketLineSelectionSource.CUSTOMER_SELECTED,
                stake,
                null,
                null,
                null
            )),
            new TicketPrintMoney(stake, List.of(), money("0"), stake),
            new TicketPrintQrPayload("v1", "ABCD1234", "111222", "https://tickets.test/check/ABCD1234", "payload"),
            new TicketPrintMetadata(now, Locale.FRENCH, ZoneId.of("America/Port-au-Prince"),
                TicketSaleChannel.POS_ONLINE, HTG.value(), Map.of())
        );
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}
