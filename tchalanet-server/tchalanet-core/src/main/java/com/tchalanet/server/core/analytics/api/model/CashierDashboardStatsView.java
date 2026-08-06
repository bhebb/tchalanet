package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Analytics projection for the cashier dashboard (POS and web).
 *
 * <p>Scoped to a single seller-terminal for one tenant business date.
 */
public record CashierDashboardStatsView(
    LocalDate refDate,
    CashierSummaryCard today,
    List<CashierGameBreakdown> gameBreakdown,
    List<CashierDrawBreakdown> drawBreakdown) {

  public record CashierSummaryCard(
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal sellerCommission,
      BigDecimal netRevenueEstimated) {}

  public record CashierGameBreakdown(String gameCode, long ticketsSold, BigDecimal grossSales) {}

  /**
   * Per-draw figures for one seller terminal. Scoped to that terminal only — never tenant-wide
   * totals, which would expose other sellers' numbers on the POS.
   */
  public record CashierDrawBreakdown(
      UUID drawId,
      String drawChannelCode,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal sellerCommission,
      BigDecimal netRevenueEstimated) {}
}
