package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

/** Query to get detailed ticket information including lines. */
public record GetTicketDetailsQuery(
    UUID ticketId
) implements Query<ListTicketsQuery.TicketDetailsDto> {}
