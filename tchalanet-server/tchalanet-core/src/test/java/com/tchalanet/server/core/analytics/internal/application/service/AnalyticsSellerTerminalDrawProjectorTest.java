package com.tchalanet.server.core.analytics.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementCreatedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnalyticsSellerTerminalDrawProjectorTest {

  private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId SELLER_TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final DrawId DRAW_ID = DrawId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId DRAW_CHANNEL_ID =
      DrawChannelId.of(UUID.fromString("40000000-0000-0000-0000-000000000001"));
  private static final TicketId TICKET_ID = TicketId.of(UUID.fromString("50000000-0000-0000-0000-000000000001"));
  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final Instant NOW = Instant.parse("2026-06-25T10:00:00Z");
  private static final LocalDate REF_DATE = LocalDate.parse("2026-06-25");

  @Test
  void projectsSaleSnapshotsThenWinningAndPaidDeltasBySellerTerminalAndDraw() {
    var repository = mock(AnalyticsSellerTerminalDrawRepository.class);
    when(repository.findByTenantIdAndSellerTerminalIdAndDrawId(
        TENANT_ID.value(), SELLER_TERMINAL_ID.value(), DRAW_ID.value()))
        .thenReturn(Optional.empty());
    when(repository.save(any(AnalyticsSellerTerminalDrawEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var projector = new AnalyticsSellerTerminalDrawProjector(
        repository,
        Clock.fixed(NOW, ZoneOffset.UTC));

    projector.applyTicketPlaced(ticketPlaced(), REF_DATE);

    var captor = ArgumentCaptor.forClass(AnalyticsSellerTerminalDrawEntity.class);
    org.mockito.Mockito.verify(repository).save(captor.capture());
    var row = captor.getValue();

    assertThat(row.getTenantId()).isEqualTo(TENANT_ID.value());
    assertThat(row.getSellerTerminalId()).isEqualTo(SELLER_TERMINAL_ID.value());
    assertThat(row.getDrawId()).isEqualTo(DRAW_ID.value());
    assertThat(row.getTicketsSoldCount()).isEqualTo(1L);
    assertThat(row.getGrossSalesCents()).isEqualTo(1000L);
    assertThat(row.getSellerCommissionCents()).isEqualTo(150L);
    assertThat(row.getBuyerChargeCents()).isEqualTo(500L);
    assertThat(row.getTenantChargeCents()).isEqualTo(300L);
    assertThat(row.getWaivedChargeCents()).isEqualTo(200L);
    assertThat(row.getPromotionLineCount()).isEqualTo(1L);
    assertThat(row.getPromotionPayoutBaseCents()).isEqualTo(12500L);
    assertThat(row.getPromotionPotentialPayoutCents()).isEqualTo(150000L);
    assertThat(row.getNetRevenueEstimatedCents()).isEqualTo(550L);
    assertThat(row.getNetRevenuePaidBasisCents()).isEqualTo(550L);

    when(repository.findByTenantIdAndSellerTerminalIdAndDrawId(
        TENANT_ID.value(), SELLER_TERMINAL_ID.value(), DRAW_ID.value()))
        .thenReturn(Optional.of(row));

    projector.applyTicketWinningSettlementCreated(winningCreated(4000L), REF_DATE);
    projector.applyTicketPayoutPaid(payoutPaid(2500L), REF_DATE);

    assertThat(row.getWinningsCalculatedCents()).isEqualTo(4000L);
    assertThat(row.getPayoutsPaidCents()).isEqualTo(2500L);
    assertThat(row.getNetRevenueEstimatedCents()).isEqualTo(-3450L);
    assertThat(row.getNetRevenuePaidBasisCents()).isEqualTo(-1950L);
  }

  private static TicketPlacedEvent ticketPlaced() {
    return new TicketPlacedEvent(
        EventId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
        TicketPlacedEvent.CURRENT_SCHEMA,
        NOW,
        CorrelationId.of(UUID.fromString("70000000-0000-0000-0000-000000000001")),
        TENANT_ID,
        TICKET_ID,
        TicketSaleStatus.APPROVED,
        TicketSaleChannel.WEB,
        new TicketContextPayload(
            DRAW_ID,
            DRAW_CHANNEL_ID,
            SELLER_TERMINAL_ID,
            new BigDecimal("15.00"),
            new BigDecimal("1.50")),
        new TicketMoneyPayload(
            HTG,
            money("10.00"),
            money("15.00"),
            money("1500.00"),
            List.of(
                new TicketMoneyPayload.ChargeItem(
                    TicketChargeType.BUYER_SMS, money("5.00"), ChargePaidBy.BUYER, false, null),
                new TicketMoneyPayload.ChargeItem(
                    TicketChargeType.BUYER_WHATSAPP, money("3.00"), ChargePaidBy.TENANT, false, null),
                new TicketMoneyPayload.ChargeItem(
                    TicketChargeType.BUYER_SMS, money("2.00"), ChargePaidBy.BUYER, true, "WAIVE_CHARGE"))),
        List.of(
            line(1, TicketLineOrigin.CUSTOMER, TicketLinePricingSource.STANDARD, money("10.00"), money("10.00")),
            line(2, TicketLineOrigin.PROMOTION, TicketLinePricingSource.PROMOTION, money("0.00"), money("125.00"))),
        null);
  }

  private static TicketLinePlacedItem line(
      int lineNumber,
      TicketLineOrigin origin,
      TicketLinePricingSource pricingSource,
      Money stake,
      Money payoutBase) {
    return new TicketLinePlacedItem(
        TicketLineId.of(UUID.fromString("80000000-0000-0000-0000-00000000000" + lineNumber)),
        lineNumber,
        GameCode.HT_BOLET,
        BetType.MATCH_1_2D,
        "12",
        "12",
        (short) 1,
        stake,
        new BigDecimal("12.0000"),
        money(origin == TicketLineOrigin.PROMOTION ? "1500.00" : "120.00"),
        origin,
        pricingSource,
        TicketLineSelectionSource.CUSTOMER_SELECTED,
        payoutBase,
        null,
        null,
        null);
  }

  private static TicketWinningSettlementCreatedEvent winningCreated(long amountCents) {
    return new TicketWinningSettlementCreatedEvent(
        EventId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
        NOW,
        TENANT_ID,
        TICKET_ID,
        DRAW_ID,
        amountCents,
        "HTG",
        SELLER_TERMINAL_ID);
  }

  private static TicketPayoutPaidEvent payoutPaid(long amountCents) {
    return new TicketPayoutPaidEvent(
        EventId.of(UUID.fromString("90000000-0000-0000-0000-000000000002")),
        NOW,
        TENANT_ID,
        TICKET_ID,
        DRAW_ID,
        amountCents,
        "HTG",
        SELLER_TERMINAL_ID,
        null);
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), HTG);
  }
}
