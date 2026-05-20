package com.tchalanet.server.features.stats.tenantdashboard.model;

import java.math.BigDecimal;

public record TenantGameBreakdownItem(
    String gameCode,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue) {}
