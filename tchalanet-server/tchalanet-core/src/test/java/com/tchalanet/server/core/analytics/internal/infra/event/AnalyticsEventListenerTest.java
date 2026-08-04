package com.tchalanet.server.core.analytics.internal.infra.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.QueryBus;
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
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDailyProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDrawProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsReconciliationAlertService;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSelectionProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSellerTerminalDrawProjector;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsTenantProjectionLock;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AnalyticsEventListenerTest {

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
  private static final Instant NOW = Instant.parse("2026-07-31T14:00:00Z");

  @Test
  void publishesApprovedTicketToAllSaleProjectors() {
    var queryBus = org.mockito.Mockito.mock(QueryBus.class);
    var processed = org.mockito.Mockito.mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(any(), any())).thenReturn(true);
    var daily = org.mockito.Mockito.mock(AnalyticsDailyProjector.class);
    var draw = org.mockito.Mockito.mock(AnalyticsDrawProjector.class);
    var selection = org.mockito.Mockito.mock(AnalyticsSelectionProjector.class);
    var sellerTerminalDraw = org.mockito.Mockito.mock(AnalyticsSellerTerminalDrawProjector.class);
    var zones = org.mockito.Mockito.mock(TenantZoneApi.class);
    var lock = org.mockito.Mockito.mock(AnalyticsTenantProjectionLock.class);
    var alertService = org.mockito.Mockito.mock(AnalyticsReconciliationAlertService.class);
    when(zones.resolveTenantZone(TENANT_ID)).thenReturn(ZoneOffset.UTC);
    var listener =
        new AnalyticsEventListener(
            queryBus,
            processed,
            daily,
            draw,
            selection,
            sellerTerminalDraw,
            zones,
            lock,
            alertService);

    listener.onTicketPlaced(ticketPlaced());
    listener.onTicketPlacedForSelection(ticketPlaced());
    listener.onTicketPlacedForDraw(ticketPlaced());
    listener.onTicketPlacedForSellerTerminalDraw(ticketPlaced());

    var refDate = NOW.atZone(ZoneOffset.UTC).toLocalDate();
    verify(daily).applyTicketPlaced(any(), org.mockito.ArgumentMatchers.eq(refDate));
    verify(selection).applyTicketPlaced(any(), org.mockito.ArgumentMatchers.eq(refDate));
    verify(draw).applyTicketPlaced(any(), org.mockito.ArgumentMatchers.eq(refDate));
    verify(sellerTerminalDraw).applyTicketPlaced(any(), org.mockito.ArgumentMatchers.eq(refDate));
    verify(lock, org.mockito.Mockito.times(4)).acquire(TENANT_ID);
  }

  @Test
  void skipsAllProjectorsWhenTheEventWasAlreadyProcessed() {
    var queryBus = org.mockito.Mockito.mock(QueryBus.class);
    var processed = org.mockito.Mockito.mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(any(), any())).thenReturn(false);
    var daily = org.mockito.Mockito.mock(AnalyticsDailyProjector.class);
    var draw = org.mockito.Mockito.mock(AnalyticsDrawProjector.class);
    var selection = org.mockito.Mockito.mock(AnalyticsSelectionProjector.class);
    var sellerTerminalDraw = org.mockito.Mockito.mock(AnalyticsSellerTerminalDrawProjector.class);
    var zones = org.mockito.Mockito.mock(TenantZoneApi.class);
    var lock = org.mockito.Mockito.mock(AnalyticsTenantProjectionLock.class);
    var alertService = org.mockito.Mockito.mock(AnalyticsReconciliationAlertService.class);
    var listener =
        new AnalyticsEventListener(
            queryBus,
            processed,
            daily,
            draw,
            selection,
            sellerTerminalDraw,
            zones,
            lock,
            alertService);

    listener.onTicketPlaced(ticketPlaced());

    verify(daily, never()).applyTicketPlaced(any(), any());
    verify(zones, never()).resolveTenantZone(any());
    InOrder order = org.mockito.Mockito.inOrder(lock, processed);
    order.verify(lock).acquire(TENANT_ID);
    order.verify(processed).markProcessedIfAbsent(any(), any());
  }

  @Test
  void cancellationLoadsImmutableSnapshotAndReversesEveryFinancialProjection() {
    var queryBus = org.mockito.Mockito.mock(QueryBus.class);
    var processed = org.mockito.Mockito.mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(any(), any())).thenReturn(true);
    var daily = org.mockito.Mockito.mock(AnalyticsDailyProjector.class);
    var draw = org.mockito.Mockito.mock(AnalyticsDrawProjector.class);
    var selection = org.mockito.Mockito.mock(AnalyticsSelectionProjector.class);
    var sellerTerminalDraw = org.mockito.Mockito.mock(AnalyticsSellerTerminalDrawProjector.class);
    var zones = org.mockito.Mockito.mock(TenantZoneApi.class);
    var lock = org.mockito.Mockito.mock(AnalyticsTenantProjectionLock.class);
    var alertService = org.mockito.Mockito.mock(AnalyticsReconciliationAlertService.class);
    when(zones.resolveTenantZone(TENANT_ID)).thenReturn(ZoneOffset.UTC);
    var snapshot = snapshot();
    org.mockito.Mockito.doReturn(Optional.of(snapshot)).when(queryBus).ask(any());
    var listener =
        new AnalyticsEventListener(
            queryBus,
            processed,
            daily,
            draw,
            selection,
            sellerTerminalDraw,
            zones,
            lock,
            alertService);

    var cancelled =
        new TicketCancelledEvent(
            EventId.of(UUID.fromString("60000000-0000-0000-0000-000000000002")),
            NOW.plusSeconds(3600),
            TENANT_ID,
            TICKET_ID,
            com.tchalanet.server.common.types.id.UserId.of(
                UUID.fromString("90000000-0000-0000-0000-000000000001")),
            "customer requested cancellation");

    listener.onTicketCancelled(cancelled);
    listener.onTicketCancelledForDraw(cancelled);
    listener.onTicketCancelledForSellerTerminalDraw(cancelled);

    verify(daily)
        .applyTicketCancelled(
            snapshot,
            NOW.atZone(ZoneOffset.UTC).toLocalDate(),
            NOW.plusSeconds(3600).atZone(ZoneOffset.UTC).toLocalDate());
    verify(draw).applyTicketCancelled(snapshot);
    verify(sellerTerminalDraw).applyTicketCancelled(snapshot);
  }

  @Test
  void projectionFailureAlertsTheTenantAndRethrowsTheFailure() {
    var queryBus = org.mockito.Mockito.mock(QueryBus.class);
    var processed = org.mockito.Mockito.mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(any(), any())).thenReturn(true);
    var daily = org.mockito.Mockito.mock(AnalyticsDailyProjector.class);
    var draw = org.mockito.Mockito.mock(AnalyticsDrawProjector.class);
    var selection = org.mockito.Mockito.mock(AnalyticsSelectionProjector.class);
    var sellerTerminalDraw = org.mockito.Mockito.mock(AnalyticsSellerTerminalDrawProjector.class);
    var zones = org.mockito.Mockito.mock(TenantZoneApi.class);
    var lock = org.mockito.Mockito.mock(AnalyticsTenantProjectionLock.class);
    var alertService = org.mockito.Mockito.mock(AnalyticsReconciliationAlertService.class);
    when(zones.resolveTenantZone(TENANT_ID)).thenReturn(ZoneOffset.UTC);
    var failure = new IllegalStateException("projection database unavailable");
    org.mockito.Mockito.doThrow(failure).when(daily).applyTicketPlaced(any(), any());
    var listener =
        new AnalyticsEventListener(
            queryBus,
            processed,
            daily,
            draw,
            selection,
            sellerTerminalDraw,
            zones,
            lock,
            alertService);

    assertThatThrownBy(() -> listener.onTicketPlaced(ticketPlaced())).isSameAs(failure);

    verify(alertService)
        .onProjectionFailure(
            eq(
                AnalyticsTrustScope.tenant(
                    TENANT_ID,
                    NOW.atZone(ZoneOffset.UTC).toLocalDate(),
                    NOW.atZone(ZoneOffset.UTC).toLocalDate())),
            eq("DAILY"),
            eq("60000000-0000-0000-0000-000000000001"),
            eq(failure));
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
        new TicketMoneyPayload(CurrencyCode.of("HTG"), money("10.00"), money("10.00"), List.of()),
        List.of(
            new TicketLinePlacedItem(
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
                null)),
        null);
  }

  private static SalesAnalyticsTicketSnapshot snapshot() {
    return new SalesAnalyticsTicketSnapshot(
        TICKET_ID.value(),
        TENANT_ID.value(),
        SELLER_TERMINAL_ID.value(),
        DRAW_ID.value(),
        DRAW_CHANNEL_ID.value(),
        NOW,
        NOW.plusSeconds(86_400),
        NOW.atZone(ZoneOffset.UTC).toLocalDate(),
        "CANCELLED",
        NOW.plusSeconds(3600),
        null,
        "NOT_RESULTED",
        null,
        "NOT_SETTLED",
        null,
        null,
        BigDecimal.ZERO,
        null,
        null,
        null,
        money("10.00").amount(),
        BigDecimal.ZERO,
        new BigDecimal("1.50"),
        List.of(),
        List.of());
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), CurrencyCode.of("HTG"));
  }
}
