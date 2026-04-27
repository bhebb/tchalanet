package com.tchalanet.server.features.stats.tenant_dashboard.dto;

import java.math.BigDecimal;

public record TenantGameBreakdownItemDto(
    String gameCode,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue) {}
