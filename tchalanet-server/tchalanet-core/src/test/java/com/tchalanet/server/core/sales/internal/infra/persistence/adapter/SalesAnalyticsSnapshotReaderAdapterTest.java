package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketChargeJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesAnalyticsSnapshotReaderAdapterTest {

  @Test
  void loadsChargesForTheWholeTicketSetInOneQuery() {
    var ticketRepository = mock(TicketJpaRepository.class);
    var chargeRepository = mock(TicketChargeJpaRepository.class);
    var first = ticket(UUID.randomUUID());
    var second = ticket(UUID.randomUUID());
    var from = Instant.parse("2026-07-20T00:00:00Z");
    var to = from.plusSeconds(86_400);
    var tenantId = TenantId.of(UUID.randomUUID());
    when(ticketRepository.findForAnalyticsByTenantAndSoldAtRange(tenantId.value(), from, to))
        .thenReturn(List.of(first, second));
    when(chargeRepository.findByTicket_IdInOrderByTicket_IdAscChargeTypeAsc(anyList()))
        .thenReturn(List.of());

    var result =
        new SalesAnalyticsSnapshotReaderAdapter(ticketRepository, chargeRepository)
            .findTicketSnapshots(new GetSalesAnalyticsTicketSnapshotsQuery(tenantId, from, to));

    assertThat(result).hasSize(2);
    verify(chargeRepository)
        .findByTicket_IdInOrderByTicket_IdAscChargeTypeAsc(List.of(first.getId(), second.getId()));
    verifyNoMoreInteractions(chargeRepository);
  }

  private static TicketJpaEntity ticket(UUID id) {
    var ticket = mock(TicketJpaEntity.class);
    when(ticket.getId()).thenReturn(id);
    when(ticket.getLines()).thenReturn(List.of());
    when(ticket.getSaleStatus()).thenReturn(TicketSaleStatus.APPROVED);
    when(ticket.getSettlementStatus()).thenReturn(TicketSettlementStatus.NOT_SETTLED);
    return ticket;
  }
}
