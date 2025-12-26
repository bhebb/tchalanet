package com.tchalanet.server.core.sales.application.query.handler;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketDetailsDto;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Handler to get detailed ticket information including lines.
 */
@UseCase
@RequiredArgsConstructor
public class GetTicketDetailsQueryHandler implements QueryHandler<GetTicketDetailsQuery, TicketDetailsDto> {

    private final TicketReaderPort ticketReader;

    @Override
    public TicketDetailsDto handle(GetTicketDetailsQuery query) {
        // First, check if ticket exists (without lines)
        Optional<Ticket> ticketOpt = ticketReader.findById(query.ticketId());
        if (ticketOpt.isEmpty()) {
            return null;
        }
        Ticket ticket = ticketOpt.get();

        // Then, load with lines using tenantId from the ticket
        Optional<Ticket> ticketWithLinesOpt = ticketReader.findWithLinesById(query.ticketId());
        if (ticketWithLinesOpt.isEmpty()) {
            return null; // Should not happen, but safety
        }
        Ticket ticketWithLines = ticketWithLinesOpt.get();

        return mapToDto(ticketWithLines);
    }

    private TicketDetailsDto mapToDto(Ticket ticket) {
        var lines = ticket.getLines().stream()
            .map(line -> new TicketDetailsDto.TicketLineDto(
                line.gameCode(),
                line.selection(),
                line.stake(),
                line.potentialPayout()
            ))
            .toList();

        return new TicketDetailsDto(
            ticket.getId(),
            ticket.getTenantId(),
            new TicketDetailsDto.DrawRef(ticket.getDrawId()),
            ticket.getTicketCode(),
            ticket.getPublicCode(),
            ticket.getStatus(),
            ticket.getTotalAmount(),
            ticket.getCreatedAt(),
            lines
        );
    }
}
