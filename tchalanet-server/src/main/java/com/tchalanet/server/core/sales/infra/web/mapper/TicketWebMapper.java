package com.tchalanet.server.core.sales.infra.web.mapper;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.command.model.*;
import com.tchalanet.server.core.sales.application.query.model.*;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.web.model.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketWebMapper {

  public TicketResponse toTicketResponse(SellTicketResult result) {
    Ticket ticket = result.ticket();
    return new TicketResponse(
        ticket.getId().uuid(),
        ticket.getTenantId().uuid(),
        ticket.getTerminalId().uuid(),
        ticket.getDrawId().uuid(),
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
        dto.id().uuid(),
        dto.ticketCode(),
        dto.publicCode(),
        dto.status(),
        dto.totalAmount(),
        dto.createdAt(),
        dto.terminalLabel(),
        dto.drawInfo());
  }

  // New: Convert from PagedResult<Ticket> (domain) to PagedResponse<TicketSummaryResponse>
  public PagedResponse<TicketSummaryResponse> toPagedSummaryResponseFromDomain(
      ListTicketsQuery.PagedResult<Ticket> pagedResult) {
    return new PagedResponse<>(
        pagedResult.items().stream().map(this::toSummaryResponse).collect(Collectors.toList()),
        pagedResult.totalItems(),
        pagedResult.totalPages(),
        pagedResult.currentPage());
  }

  // New: map domain Ticket to TicketSummaryResponse
  public TicketSummaryResponse toSummaryResponse(Ticket ticket) {
    return new TicketSummaryResponse(
        ticket.getId().uuid(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        "Terminal "
            + (ticket.getTerminalId() != null
                ? ticket.getTerminalId().uuid().toString().substring(0, 4)
                : "-"),
        "Draw "
            + (ticket.getDrawId() != null
                ? ticket.getDrawId().uuid().toString().substring(0, 4)
                : "-"));
  }

  public TicketResponse toTicketResponse(ListTicketsQuery.TicketDetailsDto dto) {
    return new TicketResponse(
        dto.id().uuid(),
        dto.tenantId().uuid(),
        null, // terminalId not in TicketDetailsDto
        dto.draw().id().uuid(),
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
        TenantId.of(request.tenantId()),
        TerminalId.of(request.terminalId()),
        UserId.of(request.cashierId()),
        DrawId.of(request.drawId()),
        request.lines().stream()
            .map(
                line ->
                    new SellTicketCommand.LineCommand(
                        line.gameCode(), line.selection(), line.stake(), line.betType()))
            .collect(Collectors.toList()),
        request.currency());
  }

  public ListTicketsQuery toListTicketsQuery(
      TerminalId terminalId,
      DrawId drawId,
      String status,
      Instant from,
      Instant to,
      int page,
      int size) {
    var ticketStatus = status != null ? TicketStatus.valueOf(status) : null;
    return new ListTicketsQuery(
        new ListTicketsQuery.TicketFilter(
            null, // tenantId will be injected by aspect or controller if needed, or pass it as
            // param
            terminalId,
            drawId,
            ticketStatus,
            from,
            to),
        new ListTicketsQuery.PageRequest(page, size));
  }

  public CancelSaleCommand toCancelTicketCommand(TicketId ticketId, CancelTicketRequest request) {
    return new CancelSaleCommand(
        null, // tenantId will be resolved in handler
        ticketId,
        UserId.of(request.performedBy()),
        request.reason(),
        null // currency
        );
  }

  public MarkPaymentPendingCommand toMarkPaymentPendingCommand(
      TicketId ticketId, MarkPaymentPendingRequest request) {
    return new MarkPaymentPendingCommand(ticketId, request.reason(), request.performedBy());
  }

  public MarkTicketPaidCommand toMarkTicketPaidCommand(TicketId ticketId, MarkPaidRequest request) {
    return new MarkTicketPaidCommand(ticketId, request.reason(), request.performedBy());
  }

  public SellTicketResponse toSellTicketResponse(SellTicketResult result) {
    TicketResponse ticketResponse =
        result.ticket() != null ? toTicketResponse(result.ticket()) : null;
    return new SellTicketResponse(
        ticketResponse,
        result.status(),
        java.util.Collections.emptyList(),
        result.approvalRequestId());
  }

  public CancelSaleResponse toCancelSaleResponse(CancelSaleResult result) {
    TicketResponse ticketResponse = toTicketResponse(result.ticket());
    List<com.tchalanet.server.core.sales.infra.web.model.LimitNotice> warnings =
        result.warnings().stream().map(this::mapLimitNotice).collect(Collectors.toList());
    return new CancelSaleResponse(ticketResponse, result.status(), warnings);
  }

  private com.tchalanet.server.core.sales.infra.web.model.LimitNotice mapLimitNotice(
      com.tchalanet.server.core.sales.application.command.model.LimitNotice notice) {
    return new com.tchalanet.server.core.sales.infra.web.model.LimitNotice(
        notice.ruleKey(),
        notice.message(),
        notice.targetApplied(),
        notice.selectionKey(),
        notice.currentValue(),
        notice.limitValue());
  }

  public TicketResponse toTicketResponse(Ticket ticket) {
    return new TicketResponse(
        ticket.getId().uuid(),
        ticket.getTenantId().uuid(),
        ticket.getTerminalId().uuid(),
        ticket.getDrawId().uuid(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        ticket.getLines().stream().map(this::toLineResponse).collect(Collectors.toList()));
  }
}
