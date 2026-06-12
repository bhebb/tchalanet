package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSessionRepository;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.session.api.event.SalesSessionClosedEvent;
import com.tchalanet.server.core.session.api.event.SalesSessionOpenedEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies incremental deltas to {@code analytics_session} rows.
 *
 * <p>Ticket-void counts are not projected here because {@code TicketVoidedEvent}
 * does not carry a {@code salesSessionId}. That field remains 0 until the event
 * model is enriched or a recompute job is run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSessionProjector {

  private final AnalyticsSessionRepository repo;

  public void applySessionOpened(SalesSessionOpenedEvent event) {
    LocalDate businessDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    UUID outletId   = event.outletId()   != null ? event.outletId().value()   : null;
    UUID terminalId = event.terminalId() != null ? event.terminalId().value() : null;
    UUID actorId    = event.actorId()    != null ? event.actorId().value()    : null;

    repo.ensureOpen(
        event.sessionId().value(),
        event.tenantId().value(),
        outletId,
        terminalId,
        actorId,
        businessDate,
        event.occurredAt());
  }

  public void applySessionClosed(SalesSessionClosedEvent event) {
    repo.closeSession(event.sessionId().value(), event.occurredAt());
  }

  public void applyTicketPlaced(TicketPlacedEvent event) {
    if (event.saleStatus() != TicketSaleStatus.APPROVED) {
      log.debug("analytics-session: skip PENDING ticket {}", event.ticketId().value());
      return;
    }
    if (event.context().salesSessionId() == null) {
      log.debug("analytics-session: no session on ticket {}", event.ticketId().value());
      return;
    }

    long stakeCents = toCents(event.money().stake().amount());
    long grossCents = toCents(event.money().total().amount());

    repo.increment(
        event.context().salesSessionId().value(),
        1L, 0L, grossCents, stakeCents, 0L);
  }

  private static long toCents(BigDecimal amount) {
    return amount == null ? 0L : amount.multiply(BigDecimal.valueOf(100)).longValue();
  }
}
