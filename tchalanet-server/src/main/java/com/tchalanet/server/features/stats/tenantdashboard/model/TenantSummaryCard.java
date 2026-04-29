package com.tchalanet.server.features.stats.tenantdashboard.model;

import java.math.BigDecimal;

public record TenantSummaryCard(
    long ticketsSold, BigDecimal totalSales, BigDecimal totalPayout, BigDecimal netRevenue) {}
