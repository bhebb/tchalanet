package com.tchalanet.server.features.private_dashboard.block;

public record PrivateDashboardDynamicPayload(
    SuperadminOverviewBlock superadminOverview,
    TenantAdminOverviewBlock tenantAdminOverview,
    CashierOverviewBlock cashierOverview,

    KpiBlock kpiGlobal,
    KpiBlock kpiDraws,
    KpiBlock kpiSales,

    AlertsBlock alerts,
    ActivityFeedBlock recentActivity,

    ValidationsBlock validations,      // Tenant admin / operator
    SessionBlock session,              // Cashier
    TicketsBlock recentTickets,        // Cashier
    QuickSalePreloadBlock quickSale    // Cashier
) {
    public static PrivateDashboardDynamicPayload empty() {
        return new PrivateDashboardDynamicPayload(
            SuperadminOverviewBlock.empty(),
            TenantAdminOverviewBlock.empty(),
            CashierOverviewBlock.empty(),
            KpiBlock.empty(),
            KpiBlock.empty(),
            KpiBlock.empty(),
            AlertsBlock.empty(),
            ActivityFeedBlock.empty(),
            ValidationsBlock.empty(),
            SessionBlock.empty(),
            TicketsBlock.empty(),
            QuickSalePreloadBlock.empty()
        );
    }
}
