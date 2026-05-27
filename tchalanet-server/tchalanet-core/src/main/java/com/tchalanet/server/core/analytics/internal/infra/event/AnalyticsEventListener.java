package com.tchalanet.server.core.analytics.internal.infra.event;

import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDailyProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDrawProjector;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Analytics event projector — subscribes to public domain events after commit.
 *
 * <p>Architecture rules enforced here:
 * <ul>
 *   <li>Only imports from {@code core.*.api.*} — never {@code core.*.internal.*}.</li>
 *   <li>Idempotence via {@link ProcessedEventPort} with stable handler key prefix
 *       {@code analytics.*}.</li>
 *   <li>{@link TransactionPhase#AFTER_COMMIT} — analytics projections are derived
 *       read truth, never the financial source of truth.</li>
 * </ul>
 *
 * <p>Session events (SalesSessionOpened/Closed) are not yet consumed here
 * because they are not exposed via a public API event.
 * TODO V2: add SalesSessionOpenedEvent / SalesSessionClosedEvent to core.session.api.event
 *          and wire them in AnalyticsDailyProjector.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

  static final String HANDLER_KEY_DAILY = "analytics.daily";
  static final String HANDLER_KEY_DRAW  = "analytics.draw";

  private final ProcessedEventPort        processedEvent;
  private final AnalyticsDailyProjector   dailyProjector;
  private final AnalyticsDrawProjector    drawProjector;

  // ── ticket placed ─────────────────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPlaced(TicketPlacedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPlacedEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketPlaced(event, refDate);
  }

  // ── ticket cancelled ──────────────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketCancelled(TicketCancelledEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketCancelledEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketCancelled(event, refDate);
  }

  // ── ticket settled ────────────────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketSettled(TicketResultedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketResultedEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketSettled(event, refDate);
  }

  // ── draw resulted ─────────────────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDrawResultApplied(DrawResultAppliedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate DrawResultAppliedEvent {}", event.eventId().value());
      return;
    }
    drawProjector.ensureDrawRow(event);
  }
}
