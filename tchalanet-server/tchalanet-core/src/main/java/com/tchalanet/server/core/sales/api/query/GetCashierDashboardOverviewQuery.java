package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.LocalDate;

/**
 * Returns daily ticket aggregates for the cashier dashboard overview widget.
 * RLS enforces tenant isolation — tenantId is passed for the JDBC session context.
 */
public record GetCashierDashboardOverviewQuery(
    TenantId tenantId,
    UserId cashierId,
    LocalDate businessDate
) implements Query<CashierDashboardOverviewView> {}
