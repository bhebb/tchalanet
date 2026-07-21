package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.CashierDashboardStatsView;
import java.time.LocalDate;

/**
 * Returns analytics KPIs for the cashier dashboard, scoped to a seller terminal.
 *
 * @param tenantId owning tenant
 * @param sellerTerminalId the seller-terminal whose stats are requested
 * @param refDate the business date (today in tenant-local timezone)
 */
public record GetCashierDashboardStatsQuery(
    TenantId tenantId, SellerTerminalId sellerTerminalId, LocalDate refDate)
    implements Query<CashierDashboardStatsView> {}
