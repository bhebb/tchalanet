package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.application.query.model.TicketDetailsView;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

/**
 * Get ticket details query handler.
 *
 * AUDIT: Does NOT emit audit log (v1 decision).
 * Rationale: Read-one operations are low risk; reduces log volume and improves performance.
 * Future: May be revisited in v2 based on compliance requirements.
 *
 * @see ListTicketsQueryHandler (audited for read-many)
 */
@UseCase
@RequiredArgsConstructor
public class GetTicketDetailsQueryHandler
    implements QueryHandler<GetTicketDetailsQuery, TicketDetailsView> {

  private final TicketReaderPort ticketReader;

  @Override
  public TicketDetailsView handle(GetTicketDetailsQuery query) {
    // Load ticket with lines (eager fetch to avoid N+1)
    return ticketReader.findWithLinesById(query.ticketId())
        .map(this::toView)
        .orElse(null); // Return null if not found (controller will map to 404)
  }

  private TicketDetailsView toView(Ticket t) {
    return new TicketDetailsView(
        t.getId(),
        t.getTenantId(),
        t.getTerminalId(),
        t.getDrawId(),
        t.getTicketCode(),
        t.getPublicCode(),
        t.getSaleStatus(),
        t.getResultStatus(),
        t.getSettlementStatus(),
        t.getTotalAmount(),
        t.getWinningAmount(),
        t.getResultedAt(),
        t.getCreatedAt(),
        t.getUpdatedAt(),
        t.getLines().stream()
            .map(this::toLineView)
            .collect(Collectors.toList())
    );
  }

  private TicketDetailsView.LineView toLineView(TicketLine l) {
    return new TicketDetailsView.LineView(
        l.gameCode(),
        l.betType(),
        l.betOption(),
        l.selection(),
        l.stake(),
        l.oddsSnapshot(),
        l.potentialPayout()
    );
  }
}
