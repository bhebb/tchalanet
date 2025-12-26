package com.tchalanet.server.features.private_dashboard.dynamic;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.private_dashboard.block.ActivityFeedBlock;
import com.tchalanet.server.features.private_dashboard.block.AlertsBlock;
import com.tchalanet.server.features.private_dashboard.block.KpiBlock;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.private_dashboard.block.SuperadminOverviewBlock;
import com.tchalanet.server.features.stats.platform_dashboard.application.PlatformDashboardStatsUseCase;
import com.tchalanet.server.features.stats.platform_dashboard.dto.PlatformDashboardDtos.PlatformDashboardStatsQuery;
import com.tchalanet.server.features.stats.platform_dashboard.dto.PlatformDashboardDtos.PlatformDashboardStatsResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuperadminDashboardService {

  private final PlatformDashboardStatsUseCase platformStatsUseCase;

  public PrivateDashboardDynamicPayload build(
      TenantId tenantId, UserId userId, String currentLang, PageModel pageModel) {
    LocalDate to = LocalDate.now();
    LocalDate from = to.minusDays(6);

    PlatformDashboardStatsResponse stats =
        platformStatsUseCase.handle(new PlatformDashboardStatsQuery(from, to));

    SuperadminOverviewBlock overview = buildOverviewFromPlatformStats(stats);

    KpiBlock kpiGlobal = buildGlobalKpisFromStats(stats);
    KpiBlock kpiDraws = KpiBlock.empty();
    KpiBlock kpiSales = buildSalesKpisFromStats(stats);

    AlertsBlock alerts = AlertsBlock.empty();
    ActivityFeedBlock recentActivity = ActivityFeedBlock.empty();

    return new PrivateDashboardDynamicPayload(
        overview,
        null,
        null,
        kpiGlobal,
        kpiDraws,
        kpiSales,
        alerts,
        recentActivity,
        null,
        null,
        null,
        null);
  }

  private SuperadminOverviewBlock buildOverviewFromPlatformStats(
      PlatformDashboardStatsResponse stats) {
    int totalTenants = (int) stats.summary().totalTenants();
    int activeTenants = (int) stats.summary().totalTenants(); // placeholder: use same for now
    List<String> notes = List.of("Data from stats_daily");
    return new SuperadminOverviewBlock(totalTenants, activeTenants, notes);
  }

  private KpiBlock buildGlobalKpisFromStats(PlatformDashboardStatsResponse stats) {
    KpiBlock.KpiItem tenantsItem =
        new KpiBlock.KpiItem(
            "tenants", "Tenants", String.valueOf(stats.summary().totalTenants()), "");
    KpiBlock.KpiItem outletsItem =
        new KpiBlock.KpiItem(
            "outlets", "Outlets", String.valueOf(stats.summary().totalOutlets()), "");
    KpiBlock.KpiItem cashiersItem =
        new KpiBlock.KpiItem(
            "cashiers", "Cashiers", String.valueOf(stats.summary().totalCashiers()), "");

    return new KpiBlock(List.of(tenantsItem, outletsItem, cashiersItem));
  }

  private KpiBlock buildSalesKpisFromStats(PlatformDashboardStatsResponse stats) {
    KpiBlock.KpiItem turnover =
        new KpiBlock.KpiItem(
            "turnover", "Turnover", String.valueOf(stats.summary().totalStakeCents() / 100.0), "");
    KpiBlock.KpiItem net =
        new KpiBlock.KpiItem(
            "net",
            "Net Revenue",
            String.valueOf(stats.summary().totalNetRevenueCents() / 100.0),
            "");
    return new KpiBlock(List.of(turnover, net));
  }

  // existing TODO helpers left untouched
}
