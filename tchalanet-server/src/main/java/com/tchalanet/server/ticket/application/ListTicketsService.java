package com.tchalanet.server.ticket.application;

import com.tchalanet.server.ticket.domain.model.Ticket;
import com.tchalanet.server.ticket.domain.ports.in.ListTicketsQuery;
import com.tchalanet.server.ticket.domain.ports.out.TicketRepositoryPort;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListTicketsService implements ListTicketsQuery {

  private final TicketRepositoryPort ticketRepository;

  // private final TerminalReadModelPort terminalReadModel; // To resolve terminal labels
  // private final DrawReadModelPort drawReadModel;       // To resolve draw info

  @Override
  public PagedResult<TicketSummaryDto> search(TicketFilter filter, PageRequest pageRequest) {
    PagedResult<Ticket> pagedTickets = ticketRepository.search(filter, pageRequest);

    var dtos = pagedTickets.items().stream().map(this::toDto).collect(Collectors.toList());

    return new PagedResult<>(
        dtos, pagedTickets.totalItems(), pagedTickets.totalPages(), pagedTickets.currentPage());
  }

  private TicketSummaryDto toDto(Ticket ticket) {
    // In a real implementation, these would be fetched from other domains/read models
    String terminalLabel = "Terminal " + ticket.getTerminalId().toString().substring(0, 4);
    String drawInfo = "Draw " + ticket.getDrawId().toString().substring(0, 4);

    return new TicketSummaryDto(
        ticket.getId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        terminalLabel,
        drawInfo);
  }
}
