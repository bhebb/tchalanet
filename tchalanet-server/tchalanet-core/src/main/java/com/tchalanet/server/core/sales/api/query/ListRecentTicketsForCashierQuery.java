package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.types.id.UserId;

/** Query to list recent tickets for a cashier. */
public record ListRecentTicketsForCashierQuery(UserId cashierId, int limit) {}
