package com.tchalanet.server.features.private_dashboard.dynamic;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.audit.application.query.handler.ListTenantRecentActivityQueryHandler;
import com.tchalanet.server.core.audit.application.query.model.AuditEventQuery;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleQuery;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.private_dashboard.block.ActivityFeedBlock;
import com.tchalanet.server.features.private_dashboard.block.AlertsBlock;
import com.tchalanet.server.features.private_dashboard.block.CashierOverviewBlock;
import com.tchalanet.server.features.private_dashboard.block.KpiBlock;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.private_dashboard.block.QuickSalePreloadBlock;
import com.tchalanet.server.features.private_dashboard.block.SessionBlock;
import com.tchalanet.server.features.private_dashboard.block.SuperadminOverviewBlock;
import com.tchalanet.server.features.private_dashboard.block.TenantAdminOverviewBlock;
import com.tchalanet.server.features.private_dashboard.block.TicketsBlock;
import com.tchalanet.server.features.private_dashboard.block.ValidationsBlock;
import com.tchalanet.server.features.reporting.outletperformance.GetOutletPerformanceReportHandler;
import com.tchalanet.server.features.reporting.outletperformance.GetOutletPerformanceReportQuery;
import com.tchalanet.server.features.reporting.tenantkpis.GetTenantKpisHandler;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierDashboardStatsQuery;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierDashboardStatsUseCase;
import com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDashboardStatsResponse;
import com.tchalanet.server.features.stats.tenant_dashboard.application.TenantDashboardStatsUseCase;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDailySalesPointDto;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

@Service
@RequiredArgsConstructor
public class TenantAdminDashboardService {

    private final GetTenantKpisHandler getTenantKpisHandler;
    private final TenantDashboardStatsUseCase tenantDashboardStatsUseCase;
    private final QueryBus queryBus;
    private final ListTenantRecentActivityQueryHandler listTenantRecentActivityQueryHandler;
    private final CashierDashboardStatsUseCase cashierDashboardStatsUseCase;
    private final GetOutletPerformanceReportHandler getOutletPerformanceReportHandler;

    public PrivateDashboardDynamicPayload build(
        TenantId tenantId,
        UserId userId,
        String currentLang,
        @SuppressWarnings("unused") PageModel pageModel
    ) {
        // use pageModel parameter to avoid unused-parameter warnings (it may be used later)
        var tenantStats = tenantDashboardStatsUseCase.getStats(tenantId, null, null);
        KpiBlock kpiGlobal = buildGlobalKpis(tenantId, currentLang);
        KpiBlock kpiDraws = buildDrawKpis(tenantStats);
        KpiBlock kpiSales = buildSalesKpis(tenantStats);

        ValidationsBlock validations = buildValidations(tenantId, currentLang);
        ActivityFeedBlock recentActivity = buildActivity(tenantId, userId, currentLang);
        var overview = buildOverview(tenantId, userId, currentLang, tenantStats);

        return new PrivateDashboardDynamicPayload(
            SuperadminOverviewBlock.empty(),
            overview,
            CashierOverviewBlock.empty(),
            kpiGlobal,
            kpiDraws,
            kpiSales,
            AlertsBlock.empty(),
            recentActivity,
            validations,
            SessionBlock.empty(),
            TicketsBlock.empty(),
            QuickSalePreloadBlock.empty()
        );
    }

    @SuppressWarnings("unused")
    private KpiBlock buildGlobalKpis(TenantId tenantId, String currentLang) {
        try {
            var kpisResponse = getTenantKpisHandler.handle(new com.tchalanet.server.features.reporting.tenantkpis.GetTenantKpisQuery(tenantId.uuid(), null, null));
            var snapshot = kpisResponse.snapshot();
            var kpis = snapshot.kpis();

            List<KpiBlock.KpiItem> items = new ArrayList<>();

            items.add(new KpiBlock.KpiItem("sales", "kpi.sales", kpis.totalSales() != null ? kpis.totalSales().toString() : "0", ""));
            items.add(new KpiBlock.KpiItem("payout", "kpi.payout", kpis.totalPayout() != null ? kpis.totalPayout().toString() : "0", ""));
            items.add(new KpiBlock.KpiItem("ggr", "kpi.ggr", kpis.netRevenue() != null ? kpis.netRevenue().toString() : "0", ""));

            return new KpiBlock(items);
        } catch (Exception ex) {
            return KpiBlock.empty();
        }
    }

    private KpiBlock buildDrawKpis(TenantDashboardStatsResponse statsResponse) {
        if (statsResponse == null || statsResponse.stats() == null
            || statsResponse.stats().gameBreakdown() == null
            || statsResponse.stats().gameBreakdown().isEmpty()) {
            return KpiBlock.empty();
        }
        var gameBreakdown = statsResponse.stats().gameBreakdown();
        var items = new ArrayList<KpiBlock.KpiItem>();

        // Exemple : Top 3 jeux par ventes
        items.addAll(gameBreakdown.stream()
            .sorted((a, b) -> b.totalSales().compareTo(a.totalSales()))
            .limit(3)
            .map(item -> new KpiBlock.KpiItem(
                "game_" + item.gameCode(),
                "kpi.game." + item.gameCode(),
                item.totalSales().toString(),
                "tickets: " + item.ticketsSold()
            ))
            .toList());

        return new KpiBlock(items);
    }

    private KpiBlock buildSalesKpis(TenantDashboardStatsResponse statsResponse) {
        if (statsResponse == null || statsResponse.stats().dailySales() == null || statsResponse.stats().dailySales().isEmpty()) {
            return KpiBlock.empty();
        }

        var points = statsResponse.stats().dailySales();

        var total = points.stream()
            .map(TenantDailySalesPointDto::totalSales)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        var max = points.stream()
            .map(TenantDailySalesPointDto::totalSales)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        var avg = total.divide(BigDecimal.valueOf(points.size()), RoundingMode.HALF_UP);

        var items = List.of(
            new KpiBlock.KpiItem("sales_total_period", "kpi.sales.period_total", total.toString(), ""),
            new KpiBlock.KpiItem("sales_avg_day", "kpi.sales.period_avg", avg.toString(), ""),
            new KpiBlock.KpiItem("sales_best_day", "kpi.sales.best_day", max.toString(), "")
        );

        return new KpiBlock(items);
    }

    @SuppressWarnings("unused")
    private ValidationsBlock buildValidations(TenantId tenantId, String currentLang) {
        try {
            var rule = queryBus.send(new GetAutonomyPolicyRuleQuery(tenantId, AutonomyTargetType.TENANT, tenantId.uuid()));
            if (rule == null) return ValidationsBlock.empty();

            var item = new ValidationsBlock.ValidationItem(
                rule.id() != null ? rule.id().toString() : null,
                "autonomy.policy.level." + (rule.level() != null ? rule.level().name().toLowerCase() : "unknown"),
                rule.targetType() != null ? rule.targetType().name().toLowerCase() : null,
                null,
                null,
                rule.startsAt(),
                rule.level(),
                rule.requireApprovalOnBlock(),
                rule.approvalRole(),
                rule.enabled(),
                rule.startsAt(),
                rule.endsAt()
            );

            return new ValidationsBlock(java.util.List.of(item));
        } catch (Exception ignored) {
            return ValidationsBlock.empty();
        }
    }

    @SuppressWarnings("unused")
    private ActivityFeedBlock buildActivity(TenantId tenantId, UserId userId, String currentLang) {
        try {
            var activities = listTenantRecentActivityQueryHandler.handle(new AuditEventQuery(tenantId, 20));
            if (activities == null || activities.isEmpty()) return ActivityFeedBlock.empty();

            var items = activities.stream().map(dto -> new ActivityFeedBlock.ActivityItem(
                dto.id() != null ? dto.id().toString() : null,
                dto.actorId(),
                "activity." + dto.action().name().toLowerCase(),
                dto.summary(),
                dto.entityType() != null ? dto.entityType().name() : null,
                dto.entityId(),
                dto.occurredAt()
            )).toList();

            return new ActivityFeedBlock(items);
        } catch (Exception ignored) {
            return ActivityFeedBlock.empty();
        }
    }

    private TenantAdminOverviewBlock buildOverview(TenantId tenantId, UserId userId, @SuppressWarnings("unused") String currentLang, TenantDashboardStatsResponse tenantStats) {
        try {
            // use tenant default period from tenantStats
            var from = tenantStats != null && tenantStats.stats() != null ? tenantStats.stats().fromDate() : null;
            var to = tenantStats != null && tenantStats.stats() != null ? tenantStats.stats().toDate() : null;

            var outletPerf = getOutletPerformanceReportHandler.handle(new GetOutletPerformanceReportQuery(tenantId.uuid(), from, to, null));

            var cashierStats = fetchCashierPreview(tenantId, userId, from, to);

            return new TenantAdminOverviewBlock(tenantStats, outletPerf, cashierStats);
        } catch (Exception ignored) {
            return TenantAdminOverviewBlock.empty();
        }
    }

    private List<com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDashboardStatsResponse> fetchCashierPreview(TenantId tenantId, UserId userId, LocalDate from, LocalDate to) {
        List<com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDashboardStatsResponse> result = new ArrayList<>();
        if (userId != null) {
            try {
                var cs = cashierDashboardStatsUseCase.handle(new CashierDashboardStatsQuery(tenantId.uuid(), userId.uuid(), from, to));
                result.add(cs);
            } catch (Exception e) {
                // ignore
            }
            return result;
        }

        // No userId -> fetch top N cashier summaries in one call
        try {
            int limit = 3;
            var topSummaries = cashierDashboardStatsUseCase.getTopCashierSummaries(tenantId.uuid(), from != null ? from : LocalDate.now().minusDays(6), to != null ? to : LocalDate.now(), limit);
            result.addAll(topSummaries);
        } catch (Exception ex) {
            // ignore and return what we have (possibly empty)
        }

        return result;
    }
}
