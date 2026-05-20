package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;

import java.util.List;

/**
 * Returns recent tickets sold by a given cashier, ordered by sold_at DESC.
 * TenantId is omitted — RLS enforces tenant isolation at the DB level.
 * Limit is clamped 1..20 by the handler.
 */
public record ListCashierRecentTicketsQuery(
    UserId cashierId,
    int limit
) implements Query<List<CashierRecentTicketView>> {}
