package com.tchalanet.server.core.sales.web.mapper;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.domain.ports.in.CreateTicketUseCase;
import com.tchalanet.server.core.sales.domain.ports.in.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.domain.ports.in.ListTicketsQuery;
import com.tchalanet.server.core.sales.web.dto.CreateTicketRequest;
import com.tchalanet.server.core.sales.web.dto.PagedResponse;
import com.tchalanet.server.core.sales.web.dto.TicketResponse;
import com.tchalanet.server.core.sales.web.dto.TicketSummaryResponse;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketWebMapper {

  public CreateTicketUseCase.CreateTicketCommand toCreateCommand(
      CreateTicketRequest request, UUID tenantId) {
    return new CreateTicketUseCase.CreateTicketCommand(
        tenantId,
        request.terminalId(),
        request.drawId(),
        request.lines().stream()
            .map(
                lineReq ->
                    new CreateTicketUseCase.LineCommand(
                        lineReq.gameCode(), lineReq.selection(), lineReq.stake()))
            .collect(Collectors.toList()));
  }

  public TicketResponse toTicketResponse(Ticket ticket) {
    return new TicketResponse(
        ticket.getId(),
        ticket.getTenantId(),
        ticket.getTerminalId(),
        ticket.getDrawId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        ticket.getLines().stream().map(this::toLineResponse).collect(Collectors.toList()));
  }

  private TicketResponse.LineResponse toLineResponse(TicketLine line) {
    return new TicketResponse.LineResponse(
        line.gameCode(),
        line.selection(),
        line.stake(),
        line.oddsSnapshot(),
        line.potentialPayout());
  }

  public PagedResponse<TicketSummaryResponse> toPagedSummaryResponse(
      ListTicketsQuery.PagedResult<ListTicketsQuery.TicketSummaryDto> pagedResult) {
    return new PagedResponse<>(
        pagedResult.items().stream().map(this::toSummaryResponse).collect(Collectors.toList()),
        pagedResult.totalItems(),
        pagedResult.totalPages(),
        pagedResult.currentPage());
  }

  public TicketSummaryResponse toSummaryResponse(ListTicketsQuery.TicketSummaryDto dto) {
    return new TicketSummaryResponse(
        dto.id(),
        dto.ticketCode(),
        dto.publicCode(),
        dto.status(),
        dto.totalAmount(),
        dto.createdAt(),
        dto.terminalLabel(),
        dto.drawInfo());
  }

  public TicketResponse toTicketResponse(GetTicketDetailsQuery.TicketDetailsDto dto) {
    return new TicketResponse(
        dto.id(),
        dto.tenantId(),
        null, // terminalId not in TicketDetailsDto
        dto.draw().id(),
        dto.ticketCode(),
        dto.publicCode(),
        dto.status(),
        dto.totalAmount(),
        dto.createdAt(),
        null, // updatedAt not in TicketDetailsDto
        dto.lines().stream()
            .map(
                line ->
                    new TicketResponse.LineResponse(
                        line.gameCode(),
                        line.selection(),
                        line.stake(),
                        null,
                        line.potentialPayout()))
            .collect(Collectors.toList()));
  }
}
