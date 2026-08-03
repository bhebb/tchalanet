package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsActivityDatesQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.SalesAnalyticsActivityReaderPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of the sales analytics activity coverage boundary. */
@Component
@RequiredArgsConstructor
class SalesAnalyticsActivityReaderAdapter implements SalesAnalyticsActivityReaderPort {

  private final TicketJpaRepository ticketRepository;

  @Override
  @Transactional(readOnly = true)
  public Set<LocalDate> findActivityDates(GetSalesAnalyticsActivityDatesQuery query) {
    Set<LocalDate> dates = new HashSet<>();
    ticketRepository
        .findForAnalyticsActivityByTenantAndPeriod(
            query.tenantId().value(),
            query.sellerTerminalId() != null ? query.sellerTerminalId().value() : null,
            TicketSaleStatus.APPROVED,
            query.from(),
            query.to(),
            query.from().atZone(query.businessZone()).toLocalDate(),
            query.to().minusNanos(1).atZone(query.businessZone()).toLocalDate())
        .forEach(ticket -> addActivityDates(ticket, query, dates));
    return Set.copyOf(dates);
  }

  private static void addActivityDates(
      TicketJpaEntity ticket, GetSalesAnalyticsActivityDatesQuery query, Set<LocalDate> dates) {
    addIfInRange(ticket.getSoldAt(), query, dates);
    addIfInRange(ticket.getCancelledAt(), query, dates);
    addIfInRange(ticket.getVoidedAt(), query, dates);
    addIfInRange(ticket.getResultedAt(), query, dates);
    addIfInRange(ticket.getSettledAt(), query, dates);
    addIfInRange(ticket.getPaidAt(), query, dates);
    addIfInRange(ticket.getPaidAmountAdjustedAt(), query, dates);
  }

  private static void addIfInRange(
      Instant timestamp, GetSalesAnalyticsActivityDatesQuery query, Set<LocalDate> dates) {
    if (timestamp == null || timestamp.isBefore(query.from()) || !timestamp.isBefore(query.to())) {
      return;
    }
    dates.add(timestamp.atZone(query.businessZone()).toLocalDate());
  }
}
