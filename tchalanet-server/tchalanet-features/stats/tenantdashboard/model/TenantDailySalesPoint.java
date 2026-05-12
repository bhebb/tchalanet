package com.tchalanet.server.features.stats.tenantdashboard.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantDailySalesPoint(
    LocalDate date, BigDecimal totalSales, BigDecimal totalPayout) {}
