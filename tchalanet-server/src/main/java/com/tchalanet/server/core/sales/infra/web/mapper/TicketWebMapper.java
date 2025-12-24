package com.tchalanet.server.core.sales.infra.web.mapper;

import com.tchalanet.server.core.sales.application.command.model.*;
import com.tchalanet.server.core.sales.application.query.model.*;
import com.tchalanet.server.core.sales.infra.web.model.*;
import java.time.Instant;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketWebMapper {


  public TicketResponse toTicketResponse(SellTicketResult result) {
    Ticket ticket = result.ticket();
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
  public PagedResponse<TicketSummaryResponse> toPagedSummaryResponse(ListTicketsQuery.PagedResult<ListTicketsQuery.TicketSummaryDto> pagedResult) {
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

  // New: Convert from PagedResult<Ticket> (domain) to PagedResponse<TicketSummaryResponse>
  public PagedResponse<TicketSummaryResponse> toPagedSummaryResponseFromDomain(ListTicketsQuery.PagedResult<Ticket> pagedResult) {
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

  public TicketResponse toTicketResponse(ListTicketsQuery.TicketDetailsDto dto) {
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

  // New methods for controller
  public SellTicketCommand toSellCommand(SellTicketRequest request) {
    return new SellTicketCommand(
        request.tenantId(),
        request.terminalId(),
        request.cashierId(),
        request.drawId(),
        request.lines().stream()
            .map(line -> new SellTicketCommand.LineCommand(line.gameCode(), line.selection(), line.stake(), line.betType()))
            .collect(Collectors.toList()),
        request.currency());
  }

  public ListTicketsQuery toListTicketsQuery(UUID terminalId, UUID drawId, String status, Instant from, Instant to, int page, int size) {
    var ticketStatus = status != null ? com.tchalanet.server.core.sales.domain.model.TicketStatus.valueOf(status) : null;
    return new ListTicketsQuery(
        new ListTicketsQuery.TicketFilter(null, terminalId, drawId, ticketStatus, from, to),
        new ListTicketsQuery.PageRequest(page, size));
  }

  public CancelTicketCommand toCancelTicketCommand(UUID ticketId, CancelTicketRequest request) {
    return new CancelTicketCommand(ticketId, request.reason(), request.performedBy());
  }

  public MarkPaymentPendingCommand toMarkPaymentPendingCommand(UUID ticketId, MarkPaymentPendingRequest request) {
    return new MarkPaymentPendingCommand(ticketId, request.reason(), request.performedBy());
  }

  public MarkTicketPaidCommand toMarkTicketPaidCommand(UUID ticketId, MarkPaidRequest request) {
    return new MarkTicketPaidCommand(ticketId, request.reason(), request.performedBy());
  }

  public SellTicketResponse toSellTicketResponse(SellTicketResult result) {
    TicketResponse ticketResponse = result.ticket() != null ? toTicketResponse(result) : null;
    return new SellTicketResponse(ticketResponse, result.status(), result.warnings(), result.approvalRequestId());
  }

  public CancelSaleResponse toCancelSaleResponse(CancelSaleResult result) {
    TicketResponse ticketResponse = toTicketResponse(result.ticket());
    return new CancelSaleResponse(ticketResponse, result.status(), result.warnings());
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
}
