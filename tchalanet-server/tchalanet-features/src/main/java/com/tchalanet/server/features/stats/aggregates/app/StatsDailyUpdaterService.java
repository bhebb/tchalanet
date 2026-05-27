package com.tchalanet.server.features.stats.aggregates.app;

import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.core.session.internal.domain.event.SalesSessionClosedEvent;
import com.tchalanet.server.core.session.internal.domain.event.SalesSessionOpenedEvent;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @deprecated Replaced by {@code core.analytics.internal.application.service.AnalyticsDailyProjector}.
 *             TODO: remove after analytics backfill confirmed.
 */
@Deprecated(since = "analytics-v1", forRemoval = true)
@Component
@RequiredArgsConstructor
public class StatsDailyUpdaterService {

  private final StatsDailyJpaRepository statsDailyRepo;

  @Transactional
  public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
    long stakeCents = event.money().stake().amount().multiply(BigDecimal.valueOf(100)).longValue();

    // platform
    statsDailyRepo.upsertAndIncrement(
        "platform", null, refDate, 1L, 0L, stakeCents, 0L, 0L, 0L, 0L);

    // tenant
    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, 1L, 0L, stakeCents, 0L, 0L, 0L, 0L);

    // outlet
    statsDailyRepo.upsertAndIncrement(
        "outlet", event.context().outletId().value(), refDate, 1L, 0L, stakeCents, 0L, 0L, 0L, 0L);

    // seller
    java.util.UUID sellerUserId = event.context().sellerUserId() != null
        ? event.context().sellerUserId().value() : null;
    statsDailyRepo.upsertAndIncrement(
        "seller", sellerUserId, refDate, 1L, 0L, stakeCents, 0L, 0L, 0L, 0L);
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
  public void applySessionOpened(SalesSessionOpenedEvent event, LocalDate refDate) {
    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, 0L, 0L, 0L, 0L, 0L, 1L, 0L);
  }

  @Transactional
  public void applySessionClosed(SalesSessionClosedEvent event, LocalDate refDate) {
    statsDailyRepo.upsertAndIncrement(
        "tenant", event.tenantId().value(), refDate, 0L, 0L, 0L, 0L, 0L, 0L, 1L);
  }
}
