package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Analytics projection for the cashier dashboard (POS and web).
 *
 * <p>Scoped to a single seller/user for today and optionally a short window.
 */
public record CashierDashboardStatsView(
    LocalDate refDate,
    CashierSummaryCard today,
    List<CashierGameBreakdown> gameBreakdown
) {

  public record CashierSummaryCard(
      long       ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal netRevenueEstimated
  ) {}

  public record CashierGameBreakdown(
      String     gameCode,
      long       ticketsSold,
      BigDecimal grossSales
  ) {}
}
