package com.tchalanet.server.features.stats.tenant_dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantDailySalesPointDto(
    LocalDate date, BigDecimal totalSales, BigDecimal totalPayout) {}
