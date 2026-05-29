package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.analytics.api.model.CashierDashboardStatsView;

import java.time.LocalDate;

/**
 * Returns analytics KPIs for the cashier dashboard, scoped to a seller/user.
 *
 * @param tenantId   owning tenant
 * @param sellerId   the seller user whose stats are requested
 * @param refDate    the business date (today in tenant-local timezone)
 */
public record GetCashierDashboardStatsQuery(
    TenantId  tenantId,
    UserId    sellerId,
    LocalDate refDate
) implements Query<CashierDashboardStatsView> {}
