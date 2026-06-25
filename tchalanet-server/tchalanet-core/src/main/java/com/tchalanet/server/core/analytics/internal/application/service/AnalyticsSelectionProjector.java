package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionRepository;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies per-line profitability deltas to {@code analytics_selection} rows.
 *
 * <p>Each placed ticket line is projected independently so dashboards can
 * display bet-type and selection-level stats without touching raw ticket tables.
 * Winnings are projected separately via {@code TicketWinningSettlementCreatedEvent} (handled by
 * {@link AnalyticsDailyProjector} for now; selection-level winnings require a
 * future enrichment pass).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSelectionProjector {

  private final AnalyticsSelectionRepository repo;

  public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
    if (event.saleStatus() != TicketSaleStatus.APPROVED) {
      log.debug("analytics-selection: skip PENDING ticket {}", event.ticketId().value());
      return;
    }

    UUID tenantId       = event.tenantId().value();
    UUID drawChannelId  = event.context().drawChannelId() != null
        ? event.context().drawChannelId().value() : null;

    for (TicketLinePlacedItem line : event.lines()) {
      long stakeCents = toCents(line.stakeAmount() != null ? line.stakeAmount().amount() : null);

      repo.upsertAndIncrement(
          tenantId,
          refDate,
          drawChannelId,
          line.gameCode().name(),
          line.betType().name(),
          line.betOption(),
          line.selectionKey(),
          1L,
          stakeCents,
          0L);
    }
  }

  private static long toCents(BigDecimal amount) {
    return amount == null ? 0L : amount.multiply(BigDecimal.valueOf(100)).longValue();
  }
}
