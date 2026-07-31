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
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidAmountAdjustedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
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

class AnalyticsDrawProjectorTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final DrawId DRAW_ID =
      DrawId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId DRAW_CHANNEL_ID =
      DrawChannelId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId SELLER_TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("40000000-0000-0000-0000-000000000001"));
  private static final TicketId TICKET_ID =
      TicketId.of(UUID.fromString("50000000-0000-0000-0000-000000000001"));
  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final Instant NOW = Instant.parse("2026-06-25T10:00:00Z");
  private static final LocalDate REF_DATE = LocalDate.parse("2026-06-25");

  @Test
  void postResultAmountsComeFromSinglePayoutPaidEvent() {
    var repository = mock(AnalyticsDrawRepository.class);
    when(repository.findByDrawId(DRAW_ID.value())).thenReturn(Optional.empty());
    when(repository.save(any(AnalyticsDrawEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var projector = new AnalyticsDrawProjector(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    projector.applyTicketPlaced(ticketPlaced(), REF_DATE);

    var captor = ArgumentCaptor.forClass(AnalyticsDrawEntity.class);
    org.mockito.Mockito.verify(repository).save(captor.capture());
    var row = captor.getValue();

    assertThat(row.getGrossSalesCents()).isEqualTo(1_000L);
    assertThat(row.getStakeTotalCents()).isEqualTo(1_000L);
    assertThat(row.getWinningsCalculatedCents()).isZero();
    assertThat(row.getPayoutsPaidCents()).isZero();

    when(repository.findByDrawId(DRAW_ID.value())).thenReturn(Optional.of(row));

    projector.applyTicketSettledAndPaid(payoutPaid(4_000L), REF_DATE);

    assertThat(row.getWinningsCalculatedCents()).isEqualTo(4_000L);
    assertThat(row.getPayoutsPaidCents()).isEqualTo(4_000L);
    assertThat(row.getNetRevenueEstimatedCents()).isEqualTo(-3_150L);
    assertThat(row.getNetRevenuePaidBasisCents()).isEqualTo(-3_150L);

    projector.applyTicketPayoutPaidAmountAdjusted(payoutAdjusted(4_000L, 3_500L), REF_DATE);

    assertThat(row.getWinningsCalculatedCents()).isEqualTo(4_000L);
    assertThat(row.getPayoutsPaidCents()).isEqualTo(3_500L);
    assertThat(row.getNetRevenueEstimatedCents()).isEqualTo(-3_150L);
    assertThat(row.getNetRevenuePaidBasisCents()).isEqualTo(-2_650L);
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
        new TicketMoneyPayload(HTG, money("10.00"), money("10.00"), List.of()),
        List.of(line()),
        null);
  }

  private static TicketLinePlacedItem line() {
    return new TicketLinePlacedItem(
        TicketLineId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
        1,
        GameCode.HT_BOLET,
        BetType.MATCH_2_2D,
        "12",
        "12",
        null,
        money("10.00"),
        TicketLineOrigin.CUSTOMER,
        TicketLinePricingSource.STANDARD,
        TicketLineSelectionSource.CUSTOMER_SELECTED,
        null,
        null,
        null);
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

  private static TicketPayoutPaidAmountAdjustedEvent payoutAdjusted(
      long previousAmountCents, long adjustedAmountCents) {
    return new TicketPayoutPaidAmountAdjustedEvent(
        EventId.of(UUID.fromString("90000000-0000-0000-0000-000000000003")),
        NOW,
        TENANT_ID,
        TICKET_ID,
        DRAW_ID,
        4_000L,
        previousAmountCents,
        adjustedAmountCents,
        adjustedAmountCents - previousAmountCents,
        "HTG",
        SELLER_TERMINAL_ID,
        "cashier paid less than calculated",
        null);
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), HTG);
  }

  @Test
  void aResultedDrawWithNoSaleIsNotPersisted() {
    // Seeding one row per resulted draw filled the reports with zero-valued lines carrying a raw
    // channel id and an "UNKNOWN" game — on the daily sales report they were the only row shown.
    var repo = mock(AnalyticsDrawRepository.class);
    when(repo.findByDrawId(any())).thenReturn(Optional.empty());
    var projector = new AnalyticsDrawProjector(repo, fixedClock());

    projector.ensureDrawRow(drawResultApplied());

    org.mockito.Mockito.verify(repo, org.mockito.Mockito.never())
        .save(any(AnalyticsDrawEntity.class));
  }

  @Test
  void aResultedDrawWithSalesIsStillEnriched() {
    var repo = mock(AnalyticsDrawRepository.class);
    var existing =
        AnalyticsDrawEntity.builder()
            .drawId(DRAW_ID.value())
            .tenantId(TENANT_ID.value())
            .gameCode("HT_BORLETTE")
            .ticketsSoldCount(3L)
            .build();
    when(repo.findByDrawId(any())).thenReturn(Optional.of(existing));
    when(repo.save(any(AnalyticsDrawEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    var projector = new AnalyticsDrawProjector(repo, fixedClock());

    projector.ensureDrawRow(drawResultApplied());

    var saved = ArgumentCaptor.forClass(AnalyticsDrawEntity.class);
    org.mockito.Mockito.verify(repo).save(saved.capture());
    assertThat(saved.getValue().getRefDate()).isEqualTo(REF_DATE);
    assertThat(saved.getValue().getGameCode()).isEqualTo("HT_BORLETTE");
  }

  private static Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  private static DrawResultAppliedEvent drawResultApplied() {
    return new DrawResultAppliedEvent(
        EventId.of(UUID.randomUUID()),
        NOW,
        TENANT_ID,
        DRAW_ID,
        REF_DATE,
        ResultSlotId.of(UUID.randomUUID()),
        DrawResultId.of(UUID.randomUUID()),
        DRAW_CHANNEL_ID);
  }
}
