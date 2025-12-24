package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketSummaryDto;
import com.tchalanet.server.core.sales.domain.model.Ticket;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTicketsQueryHandler implements QueryHandler<ListTicketsQuery, PagedResult<TicketSummaryDto>> {

    private final TicketReaderPort ticketRepository;

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
            null,  // terminalLabel
            null   // drawInfo
        );
    }
}
