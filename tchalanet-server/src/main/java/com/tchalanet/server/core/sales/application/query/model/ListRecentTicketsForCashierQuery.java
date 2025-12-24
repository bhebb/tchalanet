package com.tchalanet.server.core.sales.application.query.model;

import java.util.UUID;

/** Query to list recent tickets for a cashier. */
public record ListRecentTicketsForCashierQuery(
    UUID cashierId,
    int limit,
    UUID tenantId // optional, for explicit filtering if needed
) {}
