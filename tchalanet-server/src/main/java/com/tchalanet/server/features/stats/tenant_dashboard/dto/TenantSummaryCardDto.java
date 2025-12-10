package com.tchalanet.server.features.stats.tenant_dashboard.dto;

import java.math.BigDecimal;

public record TenantSummaryCardDto(
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue
) { }
