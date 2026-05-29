package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ensures an {@code analytics_draw} row exists for every resulted draw.
 *
 * <p>The row is created with zero counters; downstream ticket-settlement events
 * (TicketResultedEvent) update the winnings columns via the daily projector.
 * The draw row exists primarily to serve per-draw breakdown queries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDrawProjector {

  private final AnalyticsDrawRepository repo;
  private final Clock clock;

  /**
   * Ensure a draw row exists. Idempotent — does nothing if the row already
   * exists (the findByDrawId short-circuit is safe because idempotence is
   * already guaranteed at the listener level via ProcessedEventPort).
   */
  public void ensureDrawRow(DrawResultAppliedEvent event) {
    UUID drawId = event.drawId().value();
    if (repo.findByDrawId(drawId).isPresent()) {
      log.debug("analytics_draw row already exists for draw {}", drawId);
      return;
    }

    Instant now = Instant.now(clock);
    AnalyticsDrawEntity entity = AnalyticsDrawEntity.builder()
        .drawId(drawId)
        .tenantId(event.tenantId().value())
        .gameCode("UNKNOWN") // enriched by recompute or future TicketPlaced aggregation
        .drawChannelCode(event.drawChannelId() != null ? event.drawChannelId().value().toString() : null)
        .scheduledAt(event.occurredAt())
        .refDate(event.drawDate())
        .ticketsSoldCount(0L)
        .ticketsCancelledCount(0L)
        .grossSalesCents(0L)
        .stakeTotalCents(0L)
        .winningsCalculatedCents(0L)
        .payoutsPaidCents(0L)
        .createdAt(now)
        .updatedAt(now)
        .build();

    repo.save(entity);
    log.debug("analytics_draw row created for draw {}", drawId);
  }
}
