package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;

/** Query to get detailed ticket information including lines. */
public record GetTicketDetailsQuery(TicketId ticketId)
    implements Query<TicketDetailsView> {}
