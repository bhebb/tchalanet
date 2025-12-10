package com.tchalanet.server.features.stats.cashier_dashboard.application;

import java.time.LocalDate;
import java.util.UUID;

public record CashierDashboardStatsQuery(
    UUID tenantId,
    UUID cashierId,
    LocalDate fromDate,
    LocalDate toDate
) {}

