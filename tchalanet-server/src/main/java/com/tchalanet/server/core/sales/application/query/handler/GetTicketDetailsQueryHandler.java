package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import lombok.RequiredArgsConstructor;

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
    implements QueryHandler<GetTicketDetailsQuery, Ticket> {

  private final TicketReaderPort ticketReader;

  @Override
  public Ticket handle(GetTicketDetailsQuery query) {
    // Load ticket with lines (eager fetch to avoid N+1)
    return ticketReader.findWithLinesById(query.ticketId())
        .orElse(null); // Return null if not found (controller will map to 404)
  }
}
