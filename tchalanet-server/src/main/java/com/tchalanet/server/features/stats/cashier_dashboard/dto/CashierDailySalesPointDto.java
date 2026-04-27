package com.tchalanet.server.features.stats.cashier_dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashierDailySalesPointDto(
    LocalDate date, BigDecimal totalSales, BigDecimal totalPayout) {}
