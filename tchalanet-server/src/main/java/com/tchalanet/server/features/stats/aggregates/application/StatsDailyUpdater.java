package com.tchalanet.server.features.stats.aggregates.application;

import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.event.TicketResultedEvent;
import com.tchalanet.server.core.session.domain.event.SessionClosedEvent;
import com.tchalanet.server.core.session.domain.event.SessionOpenedEvent;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StatsDailyUpdater {

  private final StatsDailyJpaRepository statsDailyRepo;

  @Transactional
  public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
    // platform
    statsDailyRepo.upsertAndIncrement(
        "platform", null, refDate, 1L, 0L, event.stakeCents(), 0L, 0L, 0L, 0L);

    // tenant
    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, 1L, 0L, event.stakeCents(), 0L, 0L, 0L, 0L);

    // outlet
    statsDailyRepo.upsertAndIncrement(
        "outlet", event.outletId().uuid(), refDate, 1L, 0L, event.stakeCents(), 0L, 0L, 0L, 0L);

    // cashier
    statsDailyRepo.upsertAndIncrement(
        "cashier", event.cashierId(), refDate, 1L, 0L, event.stakeCents(), 0L, 0L, 0L, 0L);
  }

  @Transactional
  public void applyTicketCancelled(TicketCancelledEvent event, LocalDate refDate) {
    statsDailyRepo.upsertAndIncrement("platform", null, refDate, -1L, 1L, 0L, 0L, 0L, 0L, 0L);

    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, -1L, 1L, 0L, 0L, 0L, 0L, 0L);
  }

  @Transactional
  public void applyTicketSettled(TicketResultedEvent event, LocalDate refDate) {
    BigDecimal payout = event.totalPayout();
    long winnings = payout == null ? 0L : payout.multiply(BigDecimal.valueOf(100)).longValue();

    statsDailyRepo.upsertAndIncrement(
        "platform", null, refDate, 0L, 0L, 0L, winnings, (winnings > 0L) ? 1L : 0L, 0L, 0L);

    statsDailyRepo.upsertAndIncrement(
        "tenant",
        event.tenantId().value(),
        refDate,
        0L,
        0L,
        0L,
        winnings,
        (winnings > 0L) ? 1L : 0L,
        0L,
        0L);
  }

  @Transactional
  public void applySessionOpened(SessionOpenedEvent event, LocalDate refDate) {
    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, 0L, 0L, 0L, 0L, 0L, 1L, 0L);
  }

  @Transactional
  public void applySessionClosed(SessionClosedEvent event, LocalDate refDate) {
    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, 0L, 0L, 0L, 0L, 0L, 0L, 1L);
  }
}
