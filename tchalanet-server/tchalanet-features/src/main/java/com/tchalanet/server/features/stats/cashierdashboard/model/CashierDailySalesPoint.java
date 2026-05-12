package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashierDailySalesPoint(
    LocalDate date, BigDecimal totalSales, BigDecimal totalPayout) {}
