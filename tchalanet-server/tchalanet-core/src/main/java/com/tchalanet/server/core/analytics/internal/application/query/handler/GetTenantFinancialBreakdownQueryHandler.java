package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView.DailyFinancialRow;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView.DrawFinancialRow;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView.FinancialSummary;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView.SellerTerminalDailyFinancialRow;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Handles tenant-admin financial drilldown queries. */
@UseCase
@RequiredArgsConstructor
public class GetTenantFinancialBreakdownQueryHandler
    implements QueryHandler<GetTenantFinancialBreakdownQuery, TenantFinancialBreakdownView> {

  private final AnalyticsDailyRepository dailyRepository;
  private final AnalyticsDrawRepository drawRepository;
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository;

  @Override
  public TenantFinancialBreakdownView handle(GetTenantFinancialBreakdownQuery query) {
    UUID tenantId = query.tenantId().value();
    var tenantRows = dailyRepository.findTenantRows(tenantId, query.from(), query.to());
    var drawRows = drawRepository.findByTenantIdAndRefDateBetweenOrderByRefDate(
        tenantId, query.from(), query.to());
    var sellerRows = dailyRepository.findSellerTerminalRows(tenantId, query.from(), query.to());
    var sellerDrawRows = sellerTerminalDrawRepository
        .findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(tenantId, query.from(), query.to());

    return new TenantFinancialBreakdownView(
        query.from(),
        query.to(),
        summary(tenantRows),
        tenantRows.stream().map(this::dailyRow).toList(),
        drawRows.stream()
            .sorted(Comparator
                .comparing(AnalyticsDrawEntity::getRefDate).reversed()
                .thenComparing(AnalyticsDrawEntity::getScheduledAt, Comparator.reverseOrder()))
            .limit(safeLimit(query.drawLimit()))
            .map(this::drawRow)
            .toList(),
        sellerDrawRows.stream()
            .limit(safeLimit(query.sellerTerminalLimit()))
            .map(this::sellerTerminalDrawRow)
            .toList(),
        sellerRows.stream()
            .limit(safeLimit(query.sellerTerminalLimit()))
            .map(this::sellerTerminalDailyRow)
            .toList()
    );
  }

  private FinancialSummary summary(List<AnalyticsDailyEntity> rows) {
    long tickets = 0L;
    long gross = 0L;
    long winnings = 0L;
    long payouts = 0L;
    long commission = 0L;
    long buyerCharges = 0L;
    long sellerCharges = 0L;
    long tenantCharges = 0L;
    long waivedCharges = 0L;
    long promotionLines = 0L;
    long promotionPricedLines = 0L;
    long promotionPayoutBase = 0L;
    long promotionPotentialPayout = 0L;
    long netEstimated = 0L;
    long netPaidBasis = 0L;

    for (var row : rows) {
      tickets += row.getTicketsSoldCount();
      gross += row.getGrossSalesCents();
      winnings += row.getWinningsCalculatedCents();
      payouts += row.getPayoutsPaidCents();
      commission += row.getSellerCommissionCents();
      buyerCharges += row.getBuyerChargeCents();
      sellerCharges += row.getSellerChargeCents();
      tenantCharges += row.getTenantChargeCents();
      waivedCharges += row.getWaivedChargeCents();
      promotionLines += row.getPromotionLineCount();
      promotionPricedLines += row.getPromotionPricedLineCount();
      promotionPayoutBase += row.getPromotionPayoutBaseCents();
      promotionPotentialPayout += row.getPromotionPotentialPayoutCents();
      netEstimated += row.getNetRevenueEstimatedCents();
      netPaidBasis += row.getNetRevenuePaidBasisCents();
    }

    return new FinancialSummary(
        tickets,
        fromCents(gross),
        fromCents(winnings),
        fromCents(payouts),
        fromCents(commission),
        fromCents(buyerCharges),
        fromCents(sellerCharges),
        fromCents(tenantCharges),
        fromCents(waivedCharges),
        promotionLines,
        promotionPricedLines,
        fromCents(promotionPayoutBase),
        fromCents(promotionPotentialPayout),
        fromCents(netEstimated),
        fromCents(netPaidBasis)
    );
  }

  private DailyFinancialRow dailyRow(AnalyticsDailyEntity row) {
    return new DailyFinancialRow(
        row.getRefDate(),
        row.getTicketsSoldCount(),
        fromCents(row.getGrossSalesCents()),
        fromCents(row.getWinningsCalculatedCents()),
        fromCents(row.getPayoutsPaidCents()),
        fromCents(row.getSellerCommissionCents()),
        fromCents(row.getBuyerChargeCents()),
        fromCents(row.getSellerChargeCents()),
        fromCents(row.getTenantChargeCents()),
        fromCents(row.getWaivedChargeCents()),
        row.getPromotionLineCount(),
        row.getPromotionPricedLineCount(),
        fromCents(row.getPromotionPayoutBaseCents()),
        fromCents(row.getPromotionPotentialPayoutCents()),
        fromCents(row.getNetRevenueEstimatedCents()),
        fromCents(row.getNetRevenuePaidBasisCents())
    );
  }

  private DrawFinancialRow drawRow(AnalyticsDrawEntity row) {
    return new DrawFinancialRow(
        row.getDrawId(),
        row.getRefDate(),
        row.getScheduledAt(),
        row.getGameCode(),
        row.getDrawChannelCode(),
        row.getTicketsSoldCount(),
        fromCents(row.getGrossSalesCents()),
        fromCents(row.getWinningsCalculatedCents()),
        fromCents(row.getPayoutsPaidCents()),
        fromCents(row.getSellerCommissionCents()),
        fromCents(row.getBuyerChargeCents()),
        fromCents(row.getSellerChargeCents()),
        fromCents(row.getTenantChargeCents()),
        fromCents(row.getWaivedChargeCents()),
        row.getPromotionLineCount(),
        row.getPromotionPricedLineCount(),
        fromCents(row.getPromotionPayoutBaseCents()),
        fromCents(row.getPromotionPotentialPayoutCents()),
        fromCents(row.getNetRevenueEstimatedCents()),
        fromCents(row.getNetRevenuePaidBasisCents())
    );
  }

  private SellerTerminalDailyFinancialRow sellerTerminalDailyRow(AnalyticsDailyEntity row) {
    return new SellerTerminalDailyFinancialRow(
        row.getDimensionId(),
        row.getRefDate(),
        row.getTicketsSoldCount(),
        fromCents(row.getGrossSalesCents()),
        fromCents(row.getSellerCommissionCents()),
        fromCents(row.getBuyerChargeCents()),
        fromCents(row.getSellerChargeCents()),
        fromCents(row.getTenantChargeCents()),
        fromCents(row.getWaivedChargeCents()),
        row.getPromotionLineCount(),
        row.getPromotionPricedLineCount(),
        fromCents(row.getPromotionPayoutBaseCents()),
        fromCents(row.getPromotionPotentialPayoutCents()),
        fromCents(row.getNetRevenueEstimatedCents()),
        fromCents(row.getNetRevenuePaidBasisCents())
    );
  }

  private SellerTerminalDrawFinancialRow sellerTerminalDrawRow(AnalyticsSellerTerminalDrawEntity row) {
    return new SellerTerminalDrawFinancialRow(
        row.getSellerTerminalId(),
        row.getDrawId(),
        row.getRefDate(),
        row.getScheduledAt(),
        row.getGameCode(),
        row.getDrawChannelCode(),
        row.getTicketsSoldCount(),
        fromCents(row.getGrossSalesCents()),
        fromCents(row.getWinningsCalculatedCents()),
        fromCents(row.getPayoutsPaidCents()),
        fromCents(row.getSellerCommissionCents()),
        fromCents(row.getBuyerChargeCents()),
        fromCents(row.getSellerChargeCents()),
        fromCents(row.getTenantChargeCents()),
        fromCents(row.getWaivedChargeCents()),
        row.getPromotionLineCount(),
        row.getPromotionPricedLineCount(),
        fromCents(row.getPromotionPayoutBaseCents()),
        fromCents(row.getPromotionPotentialPayoutCents()),
        fromCents(row.getNetRevenueEstimatedCents()),
        fromCents(row.getNetRevenuePaidBasisCents())
    );
  }

  private static long safeLimit(int requested) {
    if (requested <= 0) {
      return 50L;
    }
    return Math.min(requested, 500);
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
