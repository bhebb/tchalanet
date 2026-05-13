package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.model.TicketStatus;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.api.query.TicketSummaryView;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * List tickets query handler.
 *
 * AUDIT: Emits audit log on successful execution (v1 decision).
 * Rationale: List operations (read-many) are audited for compliance and monitoring.
 *
 * @see GetTicketDetailsQueryHandler (no audit for read-one)
 */
@UseCase
@RequiredArgsConstructor
public class ListTicketsQueryHandler
    implements QueryHandler<ListTicketsQuery, TchPage<TicketSummaryView>> {

  private final TicketReaderPort ticketRepository;

  @Override
  public TchPage<TicketSummaryView> handle(ListTicketsQuery query) {
    TchPage<Ticket> result = ticketRepository.search(query.filter(), query.pageRequest());
    var items = result.items().stream().map(this::toView).collect(Collectors.toList());

    // TODO(sales-refactor): re-enable audit emission once audit handler wiring is restored.
    return TchPage.of(
        items,
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.last(),
        result.hasNext(),
        result.hasPrevious());
  }

  private TicketSummaryView toView(Ticket ticket) {
    var status = new TicketStatus(ticket.saleStatus(), ticket.resultStatus(), ticket.settlementStatus());

    return new TicketSummaryView(
        ticket.id(),
        ticket.ticketCode(),
        ticket.publicCode(),
        status,
        ticket.money().totalAmount(),
        ticket.soldAt(),
        null,
        null);
  }
}
