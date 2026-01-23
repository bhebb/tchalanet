package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.domain.model.Ticket;

/** Query to get detailed ticket information including lines. */
public record GetTicketDetailsQuery(TicketId ticketId)
    implements Query<Ticket> {}
