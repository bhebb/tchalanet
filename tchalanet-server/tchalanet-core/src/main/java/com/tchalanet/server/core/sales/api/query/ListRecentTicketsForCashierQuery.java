package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import java.util.List;

/** Query to list recent tickets for a cashier. */
public record ListRecentTicketsForCashierQuery(UserId cashierId, int limit)
    implements Query<List<Ticket>> {}
