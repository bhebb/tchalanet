package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketSummaryDto;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTicketsQueryHandler implements com.tchalanet.server.common.app.QueryHandler<ListTicketsQuery, PagedResult<TicketSummaryDto>> {

  private final TicketWritterPort ticketRepository;

  @Override
  public PagedResult<TicketSummaryDto> handle(ListTicketsQuery query) {
    PagedResult<Ticket> result = ticketRepository.search(query.filter(), query.pageRequest());
    var items = result.items().stream().map(this::toDto).collect(Collectors.toList());
    return new PagedResult<>(items, result.totalItems(), result.totalPages(), result.currentPage());
  }

  private TicketSummaryDto toDto(Ticket ticket) {
    return new TicketSummaryDto(
        ticket.getId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        "Terminal " + (ticket.getTerminalId() != null ? ticket.getTerminalId().toString().substring(0,4) : "-"),
        "Draw " + (ticket.getDrawId() != null ? ticket.getDrawId().toString().substring(0,4) : "-")
    );
  }
}
