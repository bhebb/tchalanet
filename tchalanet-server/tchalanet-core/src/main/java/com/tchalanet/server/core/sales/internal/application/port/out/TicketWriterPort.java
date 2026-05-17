package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;

public interface TicketWriterPort {

    Ticket save(Ticket ticket);
}
