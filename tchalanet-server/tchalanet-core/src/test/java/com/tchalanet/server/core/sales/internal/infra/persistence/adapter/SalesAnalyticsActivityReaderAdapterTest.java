package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsActivityDatesQuery;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesAnalyticsActivityReaderAdapterTest {

  @Test
  void returnsBusinessDatesForAnyTicketLifecycleActivityInTheWindow() {
    var ticketRepository = mock(TicketJpaRepository.class);
    var adapter = new SalesAnalyticsActivityReaderAdapter(ticketRepository);
    var tenantId = TenantId.of(UUID.randomUUID());
    var sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());
    var from = Instant.parse("2026-07-20T00:00:00Z");
    var to = Instant.parse("2026-07-22T00:00:00Z");
    var activeTicket = mock(TicketJpaEntity.class);
    var outsideWindowTicket = mock(TicketJpaEntity.class);

    when(ticketRepository.findForAnalyticsActivityByTenantAndPeriod(
            tenantId.value(), sellerTerminalId.value(), TicketSaleStatus.APPROVED, from, to))
        .thenReturn(List.of(activeTicket, outsideWindowTicket));
    when(activeTicket.getSoldAt()).thenReturn(Instant.parse("2026-07-20T23:30:00Z"));
    when(activeTicket.getResultedAt()).thenReturn(Instant.parse("2026-07-21T01:00:00Z"));
    when(outsideWindowTicket.getSoldAt()).thenReturn(Instant.parse("2026-07-22T00:00:00Z"));

    var result =
        adapter.findActivityDates(
            new GetSalesAnalyticsActivityDatesQuery(
                tenantId, sellerTerminalId, from, to, ZoneOffset.UTC));

    assertThat(result)
        .containsExactlyInAnyOrder(
            java.time.LocalDate.of(2026, 7, 20), java.time.LocalDate.of(2026, 7, 21));
  }
}
