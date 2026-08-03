package com.tchalanet.server.core.analytics.internal.infra.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDailyProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDrawProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSelectionProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSellerTerminalDrawProjector;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsTenantProjectionLock;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidAmountAdjustedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
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

  private final ProcessedEventPort processedEvent;
  private final AnalyticsDailyProjector dailyProjector;
  private final AnalyticsDrawProjector drawProjector;
  private final AnalyticsSelectionProjector selectionProjector;
  private final AnalyticsSellerTerminalDrawProjector sellerTerminalDrawProjector;
  private final TenantZoneApi tenantZoneApi;
  private final AnalyticsTenantProjectionLock projectionLock;

  // ── ticket placed ─────────────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlaced(TicketPlacedEvent event) {
    if (!claim(HANDLER_KEY_DAILY, event)) return;
    LocalDate refDate = refDate(event);
    dailyProjector.applyTicketPlaced(event, refDate);
  }

  // ── ticket cancelled ──────────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketCancelled(TicketCancelledEvent event) {
    if (!claim(HANDLER_KEY_DAILY, event)) return;
    LocalDate refDate = refDate(event);
    dailyProjector.applyTicketCancelled(event, refDate);
  }

  // ── ticket settled and paid / reversed ────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaid(TicketPayoutPaidEvent event) {
    if (!claim(HANDLER_KEY_DAILY, event)) return;
    LocalDate refDate = refDate(event);
    dailyProjector.applyTicketSettledAndPaid(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutReversed(TicketPayoutReversedEvent event) {
    if (!claim(HANDLER_KEY_DAILY, event)) return;
    LocalDate refDate = refDate(event);
    dailyProjector.applyTicketSettlementAndPayoutReversed(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidForDraw(TicketPayoutPaidEvent event) {
    if (!claim(HANDLER_KEY_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    drawProjector.applyTicketSettledAndPaid(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidForSellerTerminalDraw(TicketPayoutPaidEvent event) {
    if (!claim(HANDLER_KEY_SELLER_TERMINAL_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    sellerTerminalDrawProjector.applyTicketSettledAndPaid(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidAmountAdjusted(TicketPayoutPaidAmountAdjustedEvent event) {
    if (!claim(HANDLER_KEY_DAILY, event)) return;
    LocalDate refDate = refDate(event);
    dailyProjector.applyTicketPayoutPaidAmountAdjusted(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidAmountAdjustedForDraw(TicketPayoutPaidAmountAdjustedEvent event) {
    if (!claim(HANDLER_KEY_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    drawProjector.applyTicketPayoutPaidAmountAdjusted(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutPaidAmountAdjustedForSellerTerminalDraw(
      TicketPayoutPaidAmountAdjustedEvent event) {
    if (!claim(HANDLER_KEY_SELLER_TERMINAL_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    sellerTerminalDrawProjector.applyTicketPayoutPaidAmountAdjusted(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutReversedForDraw(TicketPayoutReversedEvent event) {
    if (!claim(HANDLER_KEY_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    drawProjector.applyTicketSettlementAndPayoutReversed(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPayoutReversedForSellerTerminalDraw(TicketPayoutReversedEvent event) {
    if (!claim(HANDLER_KEY_SELLER_TERMINAL_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    sellerTerminalDrawProjector.applyTicketSettlementAndPayoutReversed(event, refDate);
  }

  // ── selection projector ───────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlacedForSelection(TicketPlacedEvent event) {
    if (!claim(HANDLER_KEY_SELECTION, event)) return;
    LocalDate refDate = refDate(event);
    selectionProjector.applyTicketPlaced(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlacedForDraw(TicketPlacedEvent event) {
    if (!claim(HANDLER_KEY_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    drawProjector.applyTicketPlaced(event, refDate);
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onTicketPlacedForSellerTerminalDraw(TicketPlacedEvent event) {
    if (!claim(HANDLER_KEY_SELLER_TERMINAL_DRAW, event)) return;
    LocalDate refDate = refDate(event);
    sellerTerminalDrawProjector.applyTicketPlaced(event, refDate);
  }

  // ── draw resulted ─────────────────────────────────────────────────────────

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawResultApplied(DrawResultAppliedEvent event) {
    if (!claim(HANDLER_KEY_DRAW, event)) return;
    drawProjector.ensureDrawRow(event);
  }

  private LocalDate refDate(DomainEvent event) {
    return LocalDate.ofInstant(event.occurredAt(), tenantZone(event));
  }

  private ZoneId tenantZone(DomainEvent event) {
    try {
      ZoneId zoneId = tenantZoneApi.resolveTenantZone(event.tenantId());
      return zoneId != null ? zoneId : ZoneOffset.UTC;
    } catch (RuntimeException e) {
      log.warn(
          "analytics: failed to resolve tenant timezone for {} — using UTC", event.tenantId(), e);
      return ZoneOffset.UTC;
    }
  }

  private boolean claim(String handlerKey, DomainEvent event) {
    projectionLock.acquire(event.tenantId());
    if (processedEvent.markProcessedIfAbsent(handlerKey, event.eventId().value())) {
      return true;
    }
    log.debug("analytics: duplicate event handlerKey={} eventId={}", handlerKey, event.eventId().value());
    return false;
  }
}
