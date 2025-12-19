package com.tchalanet.server.features.private_dashboard.block;

import com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDashboardStatsResponse;

public record CashierOverviewBlock(
    CashierDashboardStatsResponse stats,
    OutletSummaryDto outletSummary
) {
    public static CashierOverviewBlock empty() {
        return new CashierOverviewBlock(null, OutletSummaryDto.empty());
    }
}
