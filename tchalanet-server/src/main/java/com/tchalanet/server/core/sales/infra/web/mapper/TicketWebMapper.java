package com.tchalanet.server.core.sales.infra.web.mapper;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.application.command.model.CreateTicketCommand;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsByIdQuery;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketSummaryDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.infra.web.model.CreateTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.PagedResponse;
import com.tchalanet.server.core.sales.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.infra.web.model.TicketSummaryResponse;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketWebMapper {

  public CreateTicketCommand toCreateCommand(CreateTicketRequest request, UUID tenantId) {
    return new CreateTicketCommand(
        tenantId,
        request.terminalId(),
        request.drawId(),
        request.lines().stream()
            .map(lineReq -> new CreateTicketCommand.LineCommand(lineReq.gameCode(), lineReq.selection(), lineReq.stake()))
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

  // Convert from PagedResult<TicketSummaryDto> (query model) - existing
  public PagedResponse<TicketSummaryResponse> toPagedSummaryResponse(PagedResult<TicketSummaryDto> pagedResult) {
    return new PagedResponse<>(
        pagedResult.items().stream().map(this::toSummaryResponse).collect(Collectors.toList()),
        pagedResult.totalItems(),
        pagedResult.totalPages(),
        pagedResult.currentPage());
  }

  public TicketSummaryResponse toSummaryResponse(TicketSummaryDto dto) {
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

  // New: Convert from PagedResult<Ticket> (domain) to PagedResponse<TicketSummaryResponse>
  public PagedResponse<TicketSummaryResponse> toPagedSummaryResponseFromDomain(PagedResult<Ticket> pagedResult) {
    return new PagedResponse<>(
        pagedResult.items().stream().map(this::toSummaryResponse).collect(Collectors.toList()),
        pagedResult.totalItems(),
        pagedResult.totalPages(),
        pagedResult.currentPage());
  }

  // New: map domain Ticket to TicketSummaryResponse
  public TicketSummaryResponse toSummaryResponse(Ticket ticket) {
    return new TicketSummaryResponse(
        ticket.getId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        "Terminal " + (ticket.getTerminalId() != null ? ticket.getTerminalId().toString().substring(0,4) : "-"),
        "Draw " + (ticket.getDrawId() != null ? ticket.getDrawId().toString().substring(0,4) : "-"));
  }

  public TicketResponse toTicketResponse(GetTicketDetailsByIdQuery.TicketDetailsDto dto) {
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
