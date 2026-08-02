package com.tchalanet.server.features.pos.tickets.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintDraw;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintIdentity;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLifecycle;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintMetadata;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintMoney;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintState;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.settlement.PayoutRuleSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermsSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementWinMode;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosTicketMapperTest {

  @Test
  void exposesTheCapturedPricingTermsWithoutResolvingCurrentConfiguration() {
    var line =
        new TicketPrintLine(
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            null,
            "Boul",
            "Bòlèt",
            "12",
            "12",
            money("10.00"),
            SelectionPolicy.EXPLICIT_ONLY,
            SettlementTermsSnapshot.current(
                SelectionPolicy.EXPLICIT_ONLY,
                List.of(
                    new SettlementTermSnapshot(
                        SettlementRuleCode.MATCH_1_2D,
                        null,
                        "Boul",
                        PayoutRuleSnapshot.stakeMultiplier(new BigDecimal("20")),
                        new BigDecimal("10.00"),
                        SettlementWinMode.ALTERNATIVE,
                        SettlementTermSource.SELLER_TERMINAL_OVERRIDE))),
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            TicketLineSelectionSource.CUSTOMER_SELECTED,
            null,
            null,
            null);

    var response = new PosTicketMapper().toDetailsResponse(view(line));

    var pricing = response.lines().getFirst().pricingTerms().getFirst();
    assertThat(pricing.ruleCode()).isEqualTo(SettlementRuleCode.MATCH_1_2D);
    assertThat(pricing.payoutRuleType()).isEqualTo(PayoutRuleType.STAKE_MULTIPLIER);
    assertThat(pricing.multiplier()).isEqualByComparingTo("20");
    assertThat(pricing.source()).isEqualTo(SettlementTermSource.SELLER_TERMINAL_OVERRIDE);
  }

  private static TicketPrintView view(TicketPrintLine line) {
    var now = Instant.parse("2026-08-01T12:00:00Z");
    var currency = CurrencyCode.of("HTG");
    return new TicketPrintView(
        new TicketPrintIdentity(
            TicketId.of(UUID.randomUUID()),
            TenantId.of(UUID.randomUUID()),
            "TCK-1",
            "PUB-1",
            "VER-1"),
        new TicketPrintLifecycle(
            TicketSaleStatus.APPROVED,
            TicketResultStatus.PENDING,
            TicketSettlementStatus.NOT_SETTLED),
        TicketPrintState.notPrinted(),
        new TicketPrintDraw(
            DrawId.of(UUID.randomUUID()),
            DrawChannelId.of(UUID.randomUUID()),
            "GA_LATE",
            "GA_LATE",
            "Georgia",
            "America/New_York",
            "Late",
            "Georgia · Late",
            LocalDate.of(2026, 8, 1),
            now,
            now),
        null,
        null,
        List.of(line),
        new TicketPrintMoney(money("10.00"), List.of(), money("0.00"), money("10.00")),
        null,
        new TicketPrintMetadata(
            now, Locale.FRENCH, ZoneId.of("America/Port-au-Prince"), TicketSaleChannel.POS_ONLINE, "HTG", Map.of()));
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), CurrencyCode.of("HTG"));
  }
}
