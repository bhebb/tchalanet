package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

/**
 * Query to list recent tickets for a cashier.
 */
public record ListRecentTicketsForCashierQuery(
    UUID cashierId,
    int limit
) {
}
