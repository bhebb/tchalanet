package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;

public interface TicketWriterPort {

    Ticket save(Ticket ticket);

    /**
     * Forces a flush of any pending writes in the current persistence context. Required when a
     * handler must read back through a SQL view (e.g. {@code sales_ticket_print_header_v}) in the
     * same transaction as a freshly persisted ticket.
     */
    void flushPending();
}
