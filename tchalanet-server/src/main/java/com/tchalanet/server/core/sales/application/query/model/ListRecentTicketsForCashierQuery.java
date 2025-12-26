package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.UUID;

/**
 * Query to list recent tickets for a cashier.
 */
public record ListRecentTicketsForCashierQuery(
    UserId cashierId,
    int limit
) {
}
