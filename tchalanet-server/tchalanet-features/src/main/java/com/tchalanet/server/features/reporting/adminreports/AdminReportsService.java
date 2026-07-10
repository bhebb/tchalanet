package com.tchalanet.server.features.reporting.adminreports;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import com.tchalanet.server.core.sales.api.query.ListTenantTopSelectionsByPeriodQuery;
import com.tchalanet.server.core.sales.api.query.TenantTopSelectionsByPeriodView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminReportsService {

  private final QueryBus queryBus;

  public AdminReportResponses.Overview overview(TenantId tenantId, AdminReportPeriodRequest request) {
    var breakdown = breakdown(tenantId, request);
    var sellerDirectory = sellerDirectory(tenantId);
    var topSelections = topSelections(tenantId, request);
    return new AdminReportResponses.Overview(
        breakdown.from(),
        breakdown.to(),
        summary(breakdown.summary()),
        breakdown.dailyRows().stream().map(this::dailyRow).toList(),
        breakdown.drawRows().stream().map(this::drawRow).toList(),
        breakdown.sellerTerminalDailyRows().stream()
            .map(row -> sellerTerminalRow(row, Map.of(), sellerDirectory))
            .toList(),
        topSelections.topSelections().stream().map(this::topSelectionRow).toList());
  }

  public AdminReportResponses.Draws draws(TenantId tenantId, AdminReportPeriodRequest request) {
    var breakdown = breakdown(tenantId, request);
    var rows = breakdown.drawRows().stream().map(this::drawRow).toList();
    return new AdminReportResponses.Draws(
        breakdown.from(),
        breakdown.to(),
        summaryDrawRows(rows),
        rows);
  }

  public AdminReportResponses.SellerTerminals sellerTerminals(
      TenantId tenantId,
      AdminReportPeriodRequest request
  ) {
    var breakdown = breakdown(tenantId, request);
    var drawRowsBySeller = breakdown.sellerTerminalDrawRows().stream()
        .collect(Collectors.groupingBy(TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow::sellerTerminalId));
    var sellerDirectory = sellerDirectory(tenantId);
    var rows = breakdown.sellerTerminalDailyRows().stream()
        .map(row -> sellerTerminalRow(row, drawRowsBySeller, sellerDirectory))
        .toList();
    return new AdminReportResponses.SellerTerminals(
        breakdown.from(),
        breakdown.to(),
        summarySellerTerminalRows(rows),
        rows);
  }

  private TenantFinancialBreakdownView breakdown(TenantId tenantId, AdminReportPeriodRequest request) {
    return queryBus.ask(new GetTenantFinancialBreakdownQuery(
        tenantId,
        request.from(),
        request.to(),
        request.drawLimit(),
        request.sellerTerminalLimit(),
        request.drawIds(),
        request.sellerTerminalIds()));
  }

  private TenantTopSelectionsByPeriodView topSelections(TenantId tenantId, AdminReportPeriodRequest request) {
    return queryBus.ask(new ListTenantTopSelectionsByPeriodQuery(
        tenantId,
        request.from(),
        request.to(),
        request.zoneId(),
        5));
  }

  private Map<UUID, SellerTerminalSummaryRow> sellerDirectory(TenantId tenantId) {
    var page = queryBus.ask(new ListSellerTerminalsQuery(
        tenantId,
        SellerTerminalSearchCriteria.empty(),
        new TchPageRequest(PageRequest.of(0, 500))));
    return page.items().stream().collect(Collectors.toMap(
        row -> row.id().value(),
        row -> row,
        (first, ignored) -> first));
  }

  private AdminReportSummaryResponse summary(TenantFinancialBreakdownView.FinancialSummary source) {
    return new AdminReportSummaryResponse(
        source.ticketsSold(),
        source.grossSales(),
        source.winningsCalculated(),
        source.payoutsPaid(),
        source.sellerCommission(),
        source.buyerCharges(),
        source.sellerCharges(),
        source.tenantCharges(),
        source.waivedCharges(),
        source.promotionLines(),
        source.promotionPricedLines(),
        source.promotionPayoutBase(),
        source.netRevenueEstimated(),
        source.netRevenuePaidBasis());
  }

  private AdminReportSummaryResponse summaryDrawRows(List<AdminReportRows.DrawRow> rows) {
    return new AdminReportSummaryResponse(
        rows.stream().mapToLong(AdminReportRows.DrawRow::ticketsSold).sum(),
        rows.stream().map(AdminReportRows.DrawRow::grossSales).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.DrawRow::winningsCalculated).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.DrawRow::payoutsPaid).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.DrawRow::sellerCommission).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        rows.stream().map(AdminReportRows.DrawRow::tenantCharges).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        java.math.BigDecimal.ZERO,
        0L,
        0L,
        java.math.BigDecimal.ZERO,
        rows.stream().map(AdminReportRows.DrawRow::netRevenueEstimated).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.DrawRow::netRevenuePaidBasis).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
  }

  private AdminReportSummaryResponse summarySellerTerminalRows(List<AdminReportRows.SellerTerminalRow> rows) {
    return new AdminReportSummaryResponse(
        rows.stream().mapToLong(AdminReportRows.SellerTerminalRow::ticketsSold).sum(),
        rows.stream().map(AdminReportRows.SellerTerminalRow::grossSales).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        rows.stream().map(AdminReportRows.SellerTerminalRow::sellerCommission).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.SellerTerminalRow::buyerCharges).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.SellerTerminalRow::sellerCharges).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.SellerTerminalRow::tenantCharges).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.SellerTerminalRow::waivedCharges).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        0L,
        0L,
        java.math.BigDecimal.ZERO,
        rows.stream().map(AdminReportRows.SellerTerminalRow::netRevenueEstimated).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
        rows.stream().map(AdminReportRows.SellerTerminalRow::netRevenuePaidBasis).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
  }

  private AdminReportRows.DailyRow dailyRow(TenantFinancialBreakdownView.DailyFinancialRow source) {
    return new AdminReportRows.DailyRow(
        source.refDate(),
        source.ticketsSold(),
        source.grossSales(),
        source.winningsCalculated(),
        source.payoutsPaid(),
        source.sellerCommission(),
        source.tenantCharges(),
        source.netRevenueEstimated(),
        source.netRevenuePaidBasis());
  }

  private AdminReportRows.DrawRow drawRow(TenantFinancialBreakdownView.DrawFinancialRow source) {
    return new AdminReportRows.DrawRow(
        source.drawId(),
        source.refDate(),
        source.scheduledAt(),
        source.gameCode(),
        source.drawChannelCode(),
        source.ticketsSold(),
        source.grossSales(),
        source.winningsCalculated(),
        source.payoutsPaid(),
        source.sellerCommission(),
        source.tenantCharges(),
        source.netRevenueEstimated(),
        source.netRevenuePaidBasis());
  }

  private AdminReportRows.SellerTerminalRow sellerTerminalRow(
      TenantFinancialBreakdownView.SellerTerminalDailyFinancialRow source,
      Map<UUID, List<TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow>> drawRowsBySeller,
      Map<UUID, SellerTerminalSummaryRow> sellerDirectory
  ) {
    var drawRows = drawRowsBySeller.getOrDefault(source.sellerTerminalId(), List.of());
    var seller = sellerDirectory.get(source.sellerTerminalId());
    BigDecimal averageGrossSalesPerDraw = drawRows.isEmpty()
        ? BigDecimal.ZERO
        : source.grossSales().divide(BigDecimal.valueOf(drawRows.size()), 2, RoundingMode.HALF_UP);
    return new AdminReportRows.SellerTerminalRow(
        source.sellerTerminalId(),
        seller != null ? seller.terminalCode() : null,
        seller != null ? seller.displayName() : null,
        seller != null && seller.status() != null ? seller.status().name() : null,
        source.refDate(),
        source.ticketsSold(),
        source.grossSales(),
        source.sellerCommission(),
        source.buyerCharges(),
        source.sellerCharges(),
        source.tenantCharges(),
        source.waivedCharges(),
        drawRows.size(),
        averageGrossSalesPerDraw,
        source.netRevenueEstimated(),
        source.netRevenuePaidBasis());
  }

  private AdminReportRows.TopSelectionRow topSelectionRow(TenantTopSelectionsByPeriodView.SelectionItem source) {
    return new AdminReportRows.TopSelectionRow(
        source.rank(),
        source.displaySelection(),
        source.gameCode(),
        source.betType(),
        source.betOption(),
        source.lineCount(),
        source.totalStake());
  }
}
