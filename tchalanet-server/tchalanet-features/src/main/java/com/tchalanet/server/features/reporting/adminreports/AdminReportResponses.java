package com.tchalanet.server.features.reporting.adminreports;

import java.time.LocalDate;
import java.util.List;

public final class AdminReportResponses {

  private AdminReportResponses() {}

  public record AnalyticsAvailability(
      boolean available,
      String trustState,
      String trustReasonCode,
      List<LocalDate> missingBusinessDates) {}

  public record Overview(
      AnalyticsAvailability analytics,
      LocalDate from,
      LocalDate to,
      AdminReportSummaryResponse summary,
      List<AdminReportRows.DailyRow> dailyRows,
      List<AdminReportRows.DrawRow> drawRows,
      List<AdminReportRows.SellerTerminalRow> sellerTerminalRows,
      List<AdminReportRows.TopSelectionRow> topSelections) {}

  public record Draws(
      AnalyticsAvailability analytics,
      LocalDate from,
      LocalDate to,
      AdminReportSummaryResponse summary,
      List<AdminReportRows.DrawRow> rows) {}

  public record SellerTerminals(
      AnalyticsAvailability analytics,
      LocalDate from,
      LocalDate to,
      AdminReportSummaryResponse summary,
      List<AdminReportRows.SellerTerminalRow> rows) {}
}
