package com.tchalanet.server.core.analytics.internal.infra.event;

import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDailyProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsDrawProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSelectionProjector;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsSellerTerminalDrawProjector;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementCreatedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementReversedEvent;
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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

  static final String HANDLER_KEY_DAILY     = "analytics.daily";
  static final String HANDLER_KEY_DRAW      = "analytics.draw";
  static final String HANDLER_KEY_SELECTION = "analytics.selection";
  static final String HANDLER_KEY_SELLER_TERMINAL_DRAW = "analytics.seller-terminal-draw";

  private final ProcessedEventPort          processedEvent;
  private final AnalyticsDailyProjector     dailyProjector;
  private final AnalyticsDrawProjector      drawProjector;
  private final AnalyticsSelectionProjector selectionProjector;
  private final AnalyticsSellerTerminalDrawProjector sellerTerminalDrawProjector;

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

  // ── winning settlement created ────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketWinningSettlementCreated(TicketWinningSettlementCreatedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketWinningSettlementCreatedEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketWinningSettlementCreated(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketWinningSettlementReversed(TicketWinningSettlementReversedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketWinningSettlementReversedEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketWinningSettlementReversed(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketWinningSettlementCreatedForDraw(TicketWinningSettlementCreatedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketWinningSettlementCreatedEvent (draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    drawProjector.applyTicketWinningSettlementCreated(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketWinningSettlementCreatedForSellerTerminalDraw(TicketWinningSettlementCreatedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_SELLER_TERMINAL_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketWinningSettlementCreatedEvent (seller-terminal-draw) {}",
          event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    sellerTerminalDrawProjector.applyTicketWinningSettlementCreated(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketWinningSettlementReversedForDraw(TicketWinningSettlementReversedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketWinningSettlementReversedEvent (draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    drawProjector.applyTicketWinningSettlementReversed(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketWinningSettlementReversedForSellerTerminalDraw(TicketWinningSettlementReversedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_SELLER_TERMINAL_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketWinningSettlementReversedEvent (seller-terminal-draw) {}",
          event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    sellerTerminalDrawProjector.applyTicketWinningSettlementReversed(event, refDate);
  }

  // ── payout paid / reversed ────────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPayoutPaid(TicketPayoutPaidEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPayoutPaidEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketPayoutPaid(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPayoutReversed(TicketPayoutReversedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DAILY, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPayoutReversedEvent {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    dailyProjector.applyTicketPayoutReversed(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPayoutPaidForDraw(TicketPayoutPaidEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPayoutPaidEvent (draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    drawProjector.applyTicketPayoutPaid(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPayoutPaidForSellerTerminalDraw(TicketPayoutPaidEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_SELLER_TERMINAL_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPayoutPaidEvent (seller-terminal-draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    sellerTerminalDrawProjector.applyTicketPayoutPaid(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPayoutReversedForDraw(TicketPayoutReversedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPayoutReversedEvent (draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    drawProjector.applyTicketPayoutReversed(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPayoutReversedForSellerTerminalDraw(TicketPayoutReversedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_SELLER_TERMINAL_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPayoutReversedEvent (seller-terminal-draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    sellerTerminalDrawProjector.applyTicketPayoutReversed(event, refDate);
  }

  // ── selection projector ───────────────────────────────────────────────────

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPlacedForSelection(TicketPlacedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_SELECTION, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPlacedEvent (selection) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    selectionProjector.applyTicketPlaced(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPlacedForDraw(TicketPlacedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPlacedEvent (draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    drawProjector.applyTicketPlaced(event, refDate);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTicketPlacedForSellerTerminalDraw(TicketPlacedEvent event) {
    if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY_SELLER_TERMINAL_DRAW, event.eventId().value())) {
      log.debug("analytics: duplicate TicketPlacedEvent (seller-terminal-draw) {}", event.eventId().value());
      return;
    }
    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    sellerTerminalDrawProjector.applyTicketPlaced(event, refDate);
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
