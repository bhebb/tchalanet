package com.tchalanet.server.core.analytics.internal.infra.event;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDailyProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDrawProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsReconciliationAlertService;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSelectionProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSellerTerminalDrawProjector;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsTenantProjectionLock;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidAmountAdjustedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotQuery;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

/**
 * Analytics event projector — subscribes to public domain events after commit.
 *
 * <p>Architecture rules enforced here:
 *
 * <ul>
 *   <li>Only imports from {@code core.*.api.*} — never {@code core.*.internal.*}.
 *   <li>Idempotence via {@link ProcessedEventPort} with stable handler key prefix {@code
 *       analytics.*}.
 *   <li>Events are consumed with {@link EventListener} because their producers already publish them
 *       through {@code AfterCommit.run()}. Registering another {@code AFTER_COMMIT} listener from
 *       that callback can miss the event entirely.
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

  static final String HANDLER_KEY_DAILY = "analytics.daily";
  static final String HANDLER_KEY_DRAW = "analytics.draw";
  static final String HANDLER_KEY_SELECTION = "analytics.selection";
  static final String HANDLER_KEY_SELLER_TERMINAL_DRAW = "analytics.seller-terminal-draw";
  static final String HANDLER_KEY_CANCELLED_DRAW = "analytics.cancelled-draw";
  static final String HANDLER_KEY_CANCELLED_SELLER_TERMINAL_DRAW =
      "analytics.cancelled-seller-terminal-draw";

  private final QueryBus queryBus;
  private final ProcessedEventPort processedEvent;
  private final AnalyticsDailyProjector dailyProjector;
  private final AnalyticsDrawProjector drawProjector;
  private final AnalyticsSelectionProjector selectionProjector;
  private final AnalyticsSellerTerminalDrawProjector sellerTerminalDrawProjector;
  private final TenantZoneApi tenantZoneApi;
  private final AnalyticsTenantProjectionLock projectionLock;
  private final AnalyticsReconciliationAlertService alertService;

  // ── ticket placed ─────────────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlaced(TicketPlacedEvent event) {
    project(
        HANDLER_KEY_DAILY,
        event,
        "DAILY",
        () -> dailyProjector.applyTicketPlaced(event, refDate(event)));
  }

  // ── ticket cancelled ──────────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketCancelled(TicketCancelledEvent event) {
    project(
        HANDLER_KEY_DAILY,
        event,
        "DAILY",
        () -> {
          var snapshot = cancellationSnapshot(event);
          dailyProjector.applyTicketCancelled(snapshot, refDate(snapshot), refDate(event));
        });
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketCancelledForDraw(TicketCancelledEvent event) {
    project(
        HANDLER_KEY_CANCELLED_DRAW,
        event,
        "DRAW",
        () -> drawProjector.applyTicketCancelled(cancellationSnapshot(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketCancelledForSellerTerminalDraw(TicketCancelledEvent event) {
    project(
        HANDLER_KEY_CANCELLED_SELLER_TERMINAL_DRAW,
        event,
        "SELLER_TERMINAL_DRAW",
        () -> sellerTerminalDrawProjector.applyTicketCancelled(cancellationSnapshot(event)));
  }

  // ── ticket settled and paid / reversed ────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaid(TicketPayoutPaidEvent event) {
    project(
        HANDLER_KEY_DAILY,
        event,
        "DAILY",
        () -> dailyProjector.applyTicketSettledAndPaid(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutReversed(TicketPayoutReversedEvent event) {
    project(
        HANDLER_KEY_DAILY,
        event,
        "DAILY",
        () -> dailyProjector.applyTicketSettlementAndPayoutReversed(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidForDraw(TicketPayoutPaidEvent event) {
    project(
        HANDLER_KEY_DRAW,
        event,
        "DRAW",
        () -> drawProjector.applyTicketSettledAndPaid(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidForSellerTerminalDraw(TicketPayoutPaidEvent event) {
    project(
        HANDLER_KEY_SELLER_TERMINAL_DRAW,
        event,
        "SELLER_TERMINAL_DRAW",
        () -> sellerTerminalDrawProjector.applyTicketSettledAndPaid(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidAmountAdjusted(TicketPayoutPaidAmountAdjustedEvent event) {
    project(
        HANDLER_KEY_DAILY,
        event,
        "DAILY",
        () -> dailyProjector.applyTicketPayoutPaidAmountAdjusted(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidAmountAdjustedForDraw(TicketPayoutPaidAmountAdjustedEvent event) {
    project(
        HANDLER_KEY_DRAW,
        event,
        "DRAW",
        () -> drawProjector.applyTicketPayoutPaidAmountAdjusted(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidAmountAdjustedForSellerTerminalDraw(
      TicketPayoutPaidAmountAdjustedEvent event) {
    project(
        HANDLER_KEY_SELLER_TERMINAL_DRAW,
        event,
        "SELLER_TERMINAL_DRAW",
        () ->
            sellerTerminalDrawProjector.applyTicketPayoutPaidAmountAdjusted(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutReversedForDraw(TicketPayoutReversedEvent event) {
    project(
        HANDLER_KEY_DRAW,
        event,
        "DRAW",
        () -> drawProjector.applyTicketSettlementAndPayoutReversed(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutReversedForSellerTerminalDraw(TicketPayoutReversedEvent event) {
    project(
        HANDLER_KEY_SELLER_TERMINAL_DRAW,
        event,
        "SELLER_TERMINAL_DRAW",
        () ->
            sellerTerminalDrawProjector.applyTicketSettlementAndPayoutReversed(
                event, refDate(event)));
  }

  // ── selection projector ───────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlacedForSelection(TicketPlacedEvent event) {
    project(
        HANDLER_KEY_SELECTION,
        event,
        "SELECTION",
        () -> selectionProjector.applyTicketPlaced(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlacedForDraw(TicketPlacedEvent event) {
    project(
        HANDLER_KEY_DRAW,
        event,
        "DRAW",
        () -> drawProjector.applyTicketPlaced(event, refDate(event)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlacedForSellerTerminalDraw(TicketPlacedEvent event) {
    project(
        HANDLER_KEY_SELLER_TERMINAL_DRAW,
        event,
        "SELLER_TERMINAL_DRAW",
        () -> sellerTerminalDrawProjector.applyTicketPlaced(event, refDate(event)));
  }

  // ── draw resulted ─────────────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawResultApplied(DrawResultAppliedEvent event) {
    project(HANDLER_KEY_DRAW, event, "DRAW", () -> drawProjector.ensureDrawRow(event));
  }

  private LocalDate refDate(DomainEvent event) {
    return LocalDate.ofInstant(event.occurredAt(), tenantZone(event));
  }

  private LocalDate refDate(SalesAnalyticsTicketSnapshot snapshot) {
    if (snapshot.soldAt() == null) {
      throw new IllegalStateException(
          "analytics cancellation snapshot has no sale timestamp ticketId=" + snapshot.ticketId());
    }
    return LocalDate.ofInstant(snapshot.soldAt(), tenantZone(TenantId.of(snapshot.tenantId())));
  }

  private ZoneId tenantZone(DomainEvent event) {
    return tenantZone(event.tenantId());
  }

  private ZoneId tenantZone(TenantId tenantId) {
    try {
      ZoneId zoneId = tenantZoneApi.resolveTenantZone(tenantId);
      return zoneId != null ? zoneId : ZoneOffset.UTC;
    } catch (RuntimeException e) {
      log.warn("analytics: failed to resolve tenant timezone for {} — using UTC", tenantId, e);
      return ZoneOffset.UTC;
    }
  }

  private SalesAnalyticsTicketSnapshot cancellationSnapshot(TicketCancelledEvent event) {
    return queryBus
        .ask(new GetSalesAnalyticsTicketSnapshotQuery(event.ticketId()))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "analytics cancellation snapshot missing ticketId=" + event.ticketId()));
  }

  private void project(
      String handlerKey, DomainEvent event, String projection, Runnable projectionAction) {
    try {
      if (!claim(handlerKey, event)) return;
      projectionAction.run();
    } catch (RuntimeException failure) {
      var refDate = refDate(event);
      try {
        alertService.onProjectionFailure(
            AnalyticsTrustScope.tenant(event.tenantId(), refDate, refDate),
            projection,
            event.eventId().value().toString(),
            failure);
      } catch (RuntimeException alertFailure) {
        log.error(
            "analytics: failed to persist projection failure alert eventId={} projection={}",
            event.eventId().value(),
            projection,
            alertFailure);
      }
      throw failure;
    }
  }

  private boolean claim(String handlerKey, DomainEvent event) {
    projectionLock.acquire(event.tenantId());
    if (processedEvent.markProcessedIfAbsent(handlerKey, event.eventId().value())) {
      return true;
    }
    log.debug(
        "analytics: duplicate event handlerKey={} eventId={}", handlerKey, event.eventId().value());
    return false;
  }
}
