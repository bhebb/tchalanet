package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.LogAuditEventCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.application.query.model.TicketSummaryView;
import com.tchalanet.server.core.sales.domain.model.Ticket;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ListTicketsQueryHandler
    implements QueryHandler<ListTicketsQuery, TchPage<TicketSummaryView>> {

  private final TicketReaderPort ticketRepository;
  private final AuditLoggingCommandHandler auditHandler;

  @Override
  public TchPage<TicketSummaryView> handle(ListTicketsQuery query) {
    TchPage<Ticket> result = ticketRepository.search(query.filter(), query.pageRequest());
    var items = result.items().stream().map(this::toView).collect(Collectors.toList());

    // Create page with all required parameters
    var page = TchPage.of(
        items,
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.last(),
        result.hasNext(),
        result.hasPrevious()
    );

    // Emit audit event for list operation (v1 decision: audit read-many)
    emitAuditLog(query, page);

    return page;
  }

  private void emitAuditLog(ListTicketsQuery query, TchPage<TicketSummaryView> result) {
    try {
      Map<String, Object> details = new HashMap<>();
      details.put("action", "list_tickets");
      details.put("page", query.pageRequest().getPageNumber());
      details.put("size", query.pageRequest().getPageSize());
      details.put("totalElements", result.totalElements());

      // Add filters if present
      if (query.filter() != null) {
        if (query.filter().terminalId() != null) {
          details.put("filter_terminal_id", query.filter().terminalId().toString());
        }
        if (query.filter().drawId() != null) {
          details.put("filter_draw_id", query.filter().drawId().toString());
        }
        if (query.filter().status() != null) {
          details.put("filter_status", query.filter().status().toString());
        }
        if (query.filter().from() != null) {
          details.put("filter_from", query.filter().from().toString());
        }
        if (query.filter().to() != null) {
          details.put("filter_to", query.filter().to().toString());
        }
      }

      auditHandler.handle(
        new LogAuditEventCommand(
          AuditEntityType.TICKET,
          "list", // No specific entity ID for list operations
          AuditAction.LIST,
          details
        )
      );
    } catch (Exception e) {
      // Audit failures should not break the query
      log.warn("Failed to emit audit log for list_tickets operation", e);
    }
  }

  private TicketSummaryView toView(Ticket ticket) {
    var status = new com.tchalanet.server.core.sales.application.model.TicketStatus(
        ticket.getSaleStatus(),
        ticket.getResultStatus(),
        ticket.getSettlementStatus()
    );

    return new TicketSummaryView(
        ticket.getId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        status,
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        null, // terminalLabel
        null // drawInfo
        );
  }
}
