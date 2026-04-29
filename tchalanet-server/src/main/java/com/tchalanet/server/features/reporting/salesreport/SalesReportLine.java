package com.tchalanet.server.features.reporting.salesreport;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesReportLine(
    LocalDate date,
    String gameCode,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue) {}
