package com.tchalanet.server.features.reporting.tenantkpis;

import java.math.BigDecimal;

public record KpisDto(
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue,
    long activeOutlets,
    long activeCashiers,
    BigDecimal payoutRatio,          // 0.0 – 1.0
    BigDecimal avgTicketAmount,      // totalSales / ticketsSold
    long winningTicketsCount,
    BigDecimal winningTicketsRatio) {
}
