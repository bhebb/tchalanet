package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Instant;

/** Outbound Port for ticket persistence (writes + common reads used by other cores). */
public interface TicketWritterPort {
  Ticket save(Ticket ticket);

  /**
   * Archives tickets created before the cutoff date. RLS controls tenant isolation.
   *
   * @param cutoffDate The cutoff timestamp.
   * @return The number of tickets archived.
   */
  int archiveOldTickets(Instant cutoffDate);
}
