package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.api.model.AnalyticsDimensionType;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies incremental deltas to {@code analytics_daily} rows.
 *
 * <p>Business decisions (which dimensions to update, which event fields to read)
 * live here in Java — the SQL function is a pure atomic increment primitive.
 *
 * <p>Callers (AnalyticsEventListener) ensure idempotence via ProcessedEventPort
 * before invoking any method here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDailyProjector {

  private final AnalyticsDailyRepository repo;

  // ── ticket placed ─────────────────────────────────────────────────────────

  /**
   * Apply delta for a placed ticket.
   *
   * <p>Per the TicketPlacedEvent javadoc: only {@code APPROVED} tickets count
   * as official sales. {@code PENDING_APPROVAL} tickets are counted when they
   * transition to APPROVED via TicketApprovedEvent.
   */
  public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
    if (event.saleStatus() != TicketSaleStatus.APPROVED) {
      // PENDING_APPROVAL: wait for TicketApprovedEvent — do not count yet
      log.debug("analytics: skip PENDING ticket {}", event.ticketId().value());
      return;
    }

    long stakeCents = toCents(event.money().stake().amount());
    UUID tenantId   = event.tenantId().value();
    UUID outletId   = event.context().outletId() != null ? event.context().outletId().value() : null;
    UUID sellerId   = event.context().sellerUserId() != null ? event.context().sellerUserId().value() : null;

    // Platform rollup
    upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
        1, 0, stakeCents, stakeCents, 0, 0, 0, 0);

    // Tenant
    upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
        1, 0, stakeCents, stakeCents, 0, 0, 0, 0);

    // Outlet
    if (outletId != null) {
      upsert(AnalyticsDimensionType.OUTLET, outletId, tenantId, refDate,
          1, 0, stakeCents, stakeCents, 0, 0, 0, 0);
    }

    // Seller
    if (sellerId != null) {
      upsert(AnalyticsDimensionType.SELLER, sellerId, tenantId, refDate,
          1, 0, stakeCents, stakeCents, 0, 0, 0, 0);
    }
  }

  // ── ticket cancelled ──────────────────────────────────────────────────────

  public void applyTicketCancelled(TicketCancelledEvent event, LocalDate refDate) {
    // TicketCancelledEvent does not carry the original stake amount.
    // We decrement the sold count and increment the cancelled count.
    // Gross sales reversal requires a recompute from source-of-truth data
    // (migrate-feature-stats-to-core-analytics TODO).
    UUID tenantId = event.tenantId().value();

    upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
        -1, 1, 0, 0, 0, 0, 0, 0);
    upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
        -1, 1, 0, 0, 0, 0, 0, 0);
  }

  // ── ticket settled (resulted) ─────────────────────────────────────────────

  public void applyTicketSettled(TicketResultedEvent event, LocalDate refDate) {
    long winningsCents = toCents(event.totalPayout() != null ? event.totalPayout() : BigDecimal.ZERO);
    UUID tenantId      = event.tenantId().value();

    upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
        0, 0, 0, 0, winningsCents, 0, 0, 0);
    upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
        0, 0, 0, 0, winningsCents, 0, 0, 0);
  }

  // ── helper ────────────────────────────────────────────────────────────────

  private void upsert(
      AnalyticsDimensionType dimensionType,
      UUID dimensionId,
      UUID tenantId,
      LocalDate refDate,
      long ticketsSoldDelta,
      long ticketsCancelledDelta,
      long grossSalesDelta,
      long stakeTotalDelta,
      long winningsCalcDelta,
      long payoutsPaidDelta,
      long sessionsOpenedDelta,
      long sessionsClosedDelta) {

    repo.upsertAndIncrement(
        dimensionType.name(), dimensionId, tenantId, refDate,
        ticketsSoldDelta, ticketsCancelledDelta,
        grossSalesDelta, stakeTotalDelta,
        winningsCalcDelta, payoutsPaidDelta,
        sessionsOpenedDelta, sessionsClosedDelta);
  }

  private static long toCents(BigDecimal amount) {
    return amount == null ? 0L : amount.multiply(BigDecimal.valueOf(100)).longValue();
  }
}
