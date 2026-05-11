package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.domain.model.Ticket;

public interface TicketWriterPort {
  Ticket save(Ticket ticket);
}
