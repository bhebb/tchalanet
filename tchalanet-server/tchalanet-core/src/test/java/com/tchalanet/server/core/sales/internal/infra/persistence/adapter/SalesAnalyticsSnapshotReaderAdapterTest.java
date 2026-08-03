package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketChargeJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    when(
            ticketRepository.findForAnalyticsActivityByTenantAndPeriod(
                tenantId.value(),
                null,
                TicketSaleStatus.APPROVED,
                from,
                to,
                from.atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                to.minusNanos(1).atZone(java.time.ZoneOffset.UTC).toLocalDate()))
        .thenReturn(List.of(first, second));
    when(chargeRepository.findByTicket_IdInOrderByTicket_IdAscChargeTypeAsc(anyList()))
        .thenReturn(List.of());

    var result =
        new SalesAnalyticsSnapshotReaderAdapter(ticketRepository, chargeRepository)
            .findTicketSnapshots(
                new GetSalesAnalyticsTicketSnapshotsQuery(
                    tenantId,
                    from,
                    to,
                    from.atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                    to.minusNanos(1).atZone(java.time.ZoneOffset.UTC).toLocalDate()));

    assertThat(result).hasSize(2);
    verify(chargeRepository)
        .findByTicket_IdInOrderByTicket_IdAscChargeTypeAsc(List.of(first.getId(), second.getId()));
    verifyNoMoreInteractions(chargeRepository);
  }

  private static TicketJpaEntity ticket(UUID id) {
    var ticket = mock(TicketJpaEntity.class);
    when(ticket.getId()).thenReturn(id);
    when(ticket.getTenantId()).thenReturn(UUID.randomUUID());
    when(ticket.getSellerTerminalId()).thenReturn(UUID.randomUUID());
    when(ticket.getDrawId()).thenReturn(UUID.randomUUID());
    when(ticket.getDrawChannelId()).thenReturn(UUID.randomUUID());
    when(ticket.getSoldAt()).thenReturn(Instant.parse("2026-07-20T01:00:00Z"));
    when(ticket.getDrawScheduledAt()).thenReturn(Instant.parse("2026-07-20T20:00:00Z"));
    when(ticket.getDrawDate()).thenReturn(LocalDate.of(2026, 7, 20));
    when(ticket.getStakeAmount()).thenReturn(BigDecimal.ONE);
    when(ticket.getWinningAmount()).thenReturn(BigDecimal.ZERO);
    when(ticket.getPaidAmount()).thenReturn(BigDecimal.ZERO);
    when(ticket.getLines()).thenReturn(List.of());
    when(ticket.getSaleStatus()).thenReturn(TicketSaleStatus.APPROVED);
    when(ticket.getResultStatus()).thenReturn(TicketResultStatus.NOT_RESULTED);
    when(ticket.getSettlementStatus()).thenReturn(TicketSettlementStatus.NOT_SETTLED);
    return ticket;
  }
}
