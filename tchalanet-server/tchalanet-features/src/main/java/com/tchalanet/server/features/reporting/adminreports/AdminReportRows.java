package com.tchalanet.server.features.reporting.adminreports;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AdminReportRows {

  private AdminReportRows() {}

  public record DailyRow(
      LocalDate refDate,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal tenantCharges,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis) {}

  public record DrawRow(
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
      BigDecimal tenantCharges,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis) {}

  public record SellerTerminalRow(
      UUID sellerTerminalId,
      String terminalCode,
      String displayName,
      String status,
      LocalDate refDate,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal sellerCommission,
      BigDecimal buyerCharges,
      BigDecimal sellerCharges,
      BigDecimal tenantCharges,
      BigDecimal waivedCharges,
      long drawCount,
      BigDecimal averageGrossSalesPerDraw,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis) {}

  public record TopSelectionRow(
      int rank,
      String displaySelection,
      String gameCode,
      String betType,
      Short betOption,
      long lineCount,
      BigDecimal totalStake) {}
}
