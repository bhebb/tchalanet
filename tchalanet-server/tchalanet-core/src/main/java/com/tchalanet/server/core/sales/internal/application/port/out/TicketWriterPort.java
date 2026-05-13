package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.internal.domain.model.Ticket;

public interface TicketWriterPort {
  Ticket save(Ticket ticket);

  default int archiveOldTickets(java.time.Instant cutoffDate) {
    return 0;
  }
}
