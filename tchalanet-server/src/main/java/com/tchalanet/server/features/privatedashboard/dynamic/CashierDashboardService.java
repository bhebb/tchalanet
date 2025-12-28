package com.tchalanet.server.features.privatedashboard.dynamic;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.query.handler.ListRecentTicketsForCashierHandler;
import com.tchalanet.server.core.sales.application.query.model.ListRecentTicketsForCashierQuery;
import com.tchalanet.server.core.session.application.query.handler.ListCashierOpenSessionsHandler;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.privatedashboard.block.CashierOverviewBlock;
import com.tchalanet.server.features.privatedashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.privatedashboard.block.QuickSalePreloadBlock;
import com.tchalanet.server.features.privatedashboard.block.SessionBlock;
import com.tchalanet.server.features.privatedashboard.block.TicketsBlock;
import com.tchalanet.server.features.reporting.outletperformance.GetOutletPerformanceReportHandler;
import com.tchalanet.server.features.reporting.outletperformance.GetOutletPerformanceReportQuery;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierDashboardStatsQuery;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierDashboardStatsUseCase;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierDashboardService {

  private final CashierDashboardStatsUseCase cashierDashboardStatsUseCase;
  private final ListCashierOpenSessionsHandler listCashierOpenSessionsHandler;
  private final ListRecentTicketsForCashierHandler listRecentTicketsForCashierHandler;
  private final GetOutletPerformanceReportHandler getOutletPerformanceReportHandler;

  public PrivateDashboardDynamicPayload build(
      TenantId tenantId, UserId userId, String currentLang, PageModel pageModel) {
    // pageModel reserved for future use
    // reference pageModel to avoid unused-parameter warnings (may be used later)
    if (pageModel == null) {
      // no-op
    }
    CashierOverviewBlock overview = buildOverview(tenantId, userId, currentLang);
    SessionBlock session = buildSession(tenantId, userId, currentLang);
    TicketsBlock recentTickets = buildRecentTickets(tenantId, userId, currentLang);
    QuickSalePreloadBlock quickSale = buildQuickSalePreload(tenantId, userId, currentLang);

    return new PrivateDashboardDynamicPayload(
        null, // superadminOverview
        null, // tenantAdminOverview
        overview,
        null, // kpiGlobal
        null, // kpiDraws
        null, // kpiSales
        null, // alerts
        null, // recentActivity
        null, // validations
        session,
        recentTickets,
        quickSale);
  }

  private CashierOverviewBlock buildOverview(
      TenantId tenantId, UserId userId, @SuppressWarnings("unused") String currentLang) {
    try {
      var resp =
          cashierDashboardStatsUseCase.handle(
              new CashierDashboardStatsQuery(tenantId.uuid(), userId.uuid(), null, null));
      // Fill outlet summary from outlet performance report (today)
      var today = LocalDate.now();
      var perf =
          getOutletPerformanceReportHandler.handle(
              new GetOutletPerformanceReportQuery(tenantId.uuid(), today, today, null));
      var outletLine =
          perf.outlets() != null ? perf.outlets().stream().findFirst().orElse(null) : null;
      com.tchalanet.server.features.privatedashboard.block.OutletSummaryDto outletSummary;
      if (outletLine != null) {
        outletSummary =
            new com.tchalanet.server.features.privatedashboard.block.OutletSummaryDto(
                outletLine.outletId(),
                outletLine.outletCode(),
                outletLine.outletName(),
                null,
                outletLine.totalSales(),
                outletLine.totalPayout());
      } else {
        outletSummary =
            com.tchalanet.server.features.privatedashboard.block.OutletSummaryDto.empty();
      }

      return new CashierOverviewBlock(resp, outletSummary);
    } catch (Exception ignored) {
      return CashierOverviewBlock.empty();
    }
  }

  private SessionBlock buildSession(
      TenantId tenantId, UserId userId, @SuppressWarnings("unused") String currentLang) {
    try {
      var sessions =
          listCashierOpenSessionsHandler.handle(
              new com.tchalanet.server.core.session.application.query.model
                  .ListCashierOpenSessionsQuery(tenantId, userId));
      if (sessions == null || sessions.isEmpty()) return SessionBlock.empty();
      var s = sessions.stream().findFirst().orElse(null);
      return new SessionBlock(
          s.sessionId() != null ? s.sessionId().toString() : null,
          userId != null ? userId.toString() : null,
          true,
          s.openedAt() != null ? s.openedAt().toEpochMilli() : 0L);
    } catch (Exception e) {
      return SessionBlock.empty();
    }
  }

  private TicketsBlock buildRecentTickets(
      TenantId tenantId, UserId userId, @SuppressWarnings("unused") String currentLang) {
    try {
      var sessions =
          listCashierOpenSessionsHandler.handle(
              new com.tchalanet.server.core.session.application.query.model
                  .ListCashierOpenSessionsQuery(tenantId, userId));
      var sessionIds =
          sessions.stream()
              .map(
                  com.tchalanet.server.core.session.application.query.handler
                          .ListCashierOpenSessionsHandler.CashierSessionDto
                      ::sessionId)
              .toList();
      var recent =
          listRecentTicketsForCashierHandler.handle(
              new ListRecentTicketsForCashierQuery(userId, 20));
      return new TicketsBlock(
          recent.size(), 0, recent.stream().map(r -> r.getId().uuid().toString()).toList());
    } catch (Exception e) {
      return TicketsBlock.empty();
    }
  }

  private QuickSalePreloadBlock buildQuickSalePreload(
      TenantId tenantId, UserId userId, String currentLang) {
    // TODO: quick sale games/options based on draws & product config
    return QuickSalePreloadBlock.empty();
  }
}
