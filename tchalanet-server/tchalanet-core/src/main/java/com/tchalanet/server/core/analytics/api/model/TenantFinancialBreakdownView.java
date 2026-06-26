package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tenant-admin financial drilldown view.
 *
 * <p>All money values come from analytics projections built from sale-time snapshots and ticket
 * lifecycle events. The view keeps commissions, charges and promotions separated so dashboards and
 * reports can explain net revenue instead of hiding it inside one opaque number.
 */
public record TenantFinancialBreakdownView(
    LocalDate from,
    LocalDate to,
    FinancialSummary summary,
    List<DailyFinancialRow> dailyRows,
    List<DrawFinancialRow> drawRows,
    List<SellerTerminalDrawFinancialRow> sellerTerminalDrawRows,
    List<SellerTerminalDailyFinancialRow> sellerTerminalDailyRows
) {

  public record FinancialSummary(
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal buyerCharges,
      BigDecimal sellerCharges,
      BigDecimal tenantCharges,
      BigDecimal waivedCharges,
      long promotionLines,
      long promotionPricedLines,
      BigDecimal promotionPayoutBase,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}

  public record DailyFinancialRow(
      LocalDate refDate,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal buyerCharges,
      BigDecimal sellerCharges,
      BigDecimal tenantCharges,
      BigDecimal waivedCharges,
      long promotionLines,
      long promotionPricedLines,
      BigDecimal promotionPayoutBase,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}

  public record DrawFinancialRow(
      UUID drawId,
      LocalDate refDate,
      Instant scheduledAt,
      String gameCode,
      String drawChannelCode,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal buyerCharges,
      BigDecimal sellerCharges,
      BigDecimal tenantCharges,
      BigDecimal waivedCharges,
      long promotionLines,
      long promotionPricedLines,
      BigDecimal promotionPayoutBase,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}

  public record SellerTerminalDailyFinancialRow(
      UUID sellerTerminalId,
      LocalDate refDate,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal sellerCommission,
      BigDecimal buyerCharges,
      BigDecimal sellerCharges,
      BigDecimal tenantCharges,
      BigDecimal waivedCharges,
      long promotionLines,
      long promotionPricedLines,
      BigDecimal promotionPayoutBase,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}

  public record SellerTerminalDrawFinancialRow(
      UUID sellerTerminalId,
      UUID drawId,
      LocalDate refDate,
      Instant scheduledAt,
      String gameCode,
      String drawChannelCode,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal buyerCharges,
      BigDecimal sellerCharges,
      BigDecimal tenantCharges,
      BigDecimal waivedCharges,
      long promotionLines,
      long promotionPricedLines,
      BigDecimal promotionPayoutBase,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}
}
