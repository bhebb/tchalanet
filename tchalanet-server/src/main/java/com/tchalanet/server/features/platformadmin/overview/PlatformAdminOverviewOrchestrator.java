package com.tchalanet.server.features.platformadmin.overview;

import com.tchalanet.server.catalog.billing.application.query.model.GetSubscriptionGlobalStatsQuery;
import com.tchalanet.server.catalog.billing.application.query.model.SubscriptionGlobalStatsView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.GameStatsView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotStatsView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantGlobalStatsQuery;
import com.tchalanet.server.core.tenant.application.query.model.TenantGlobalStatsView;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.CatalogStats;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.CoreStats;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.CountItem;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.PlatformStats;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.SectionStatusItem;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.SubscriptionStats;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.SubscriptionStats.ByPlanItem;
import com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.TenantStats;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformAdminOverviewOrchestrator {

  private final GameCatalog gameCatalog;
  private final ResultSlotCatalog resultSlotCatalog;
  private final QueryBus queryBus;

  public PlatformAdminOverviewView buildOverview() {
    // Gather catalog stats
    GameStatsView gameStats = gameCatalog.stats();
    ResultSlotStatsView rsStats = resultSlotCatalog.stats();

    CatalogStats catalogStats =
        new CatalogStats(
            new CountItem(gameStats.total(), gameStats.active()),
            new CountItem(rsStats.total(), rsStats.active()),
            new CountItem(0, 0), // i18nGlobalKeys placeholder
            new CountItem(0, 0), // pageModelTemplates placeholder
            new CountItem(0, 0)); // themePresets placeholder

    // Gather core stats via QueryBus
    TenantGlobalStatsView tenantStats = queryBus.send(new GetTenantGlobalStatsQuery());
    SubscriptionGlobalStatsView subStats = queryBus.send(new GetSubscriptionGlobalStatsQuery());

    TenantStats tenantStatsView =
        new TenantStats(tenantStats.total(), tenantStats.active(), tenantStats.suspended());

    List<ByPlanItem> byPlanItems = new ArrayList<>();
    for (var item : subStats.byPlan()) {
      byPlanItems.add(new ByPlanItem(item.planCode(), item.total(), item.active()));
    }

    SubscriptionStats subscriptionStatsView =
        new SubscriptionStats(
            subStats.total(), subStats.active(), subStats.pastDue(), subStats.canceled(),
            byPlanItems);

    CoreStats coreStats = new CoreStats(tenantStatsView, subscriptionStatsView);

    // Platform stats (placeholders for now)
    PlatformStats platformStats =
        new PlatformStats(
            new CountItem(0, 0), // plans placeholder
            new CountItem(0, 0), // pricingRules placeholder
            new CountItem(0, 0)); // globalSettings placeholder

    // Build sections list
    List<SectionStatusItem> sections = buildSectionsList();

    return new PlatformAdminOverviewView(
        Instant.now(), catalogStats, coreStats, platformStats, sections);
  }

  private List<SectionStatusItem> buildSectionsList() {
    List<SectionStatusItem> sections = new ArrayList<>();
    sections.add(new SectionStatusItem("games", true, "/platform-admin/games"));
    sections.add(new SectionStatusItem("result_slots", true, "/platform-admin/result-slots"));
    sections.add(new SectionStatusItem("tenants", true, "/platform-admin/tenants"));
    sections.add(new SectionStatusItem("plans", true, "/platform-admin/plans"));
    sections.add(new SectionStatusItem("pricing", true, "/platform-admin/pricing"));
    sections.add(new SectionStatusItem("settings", true, "/platform-admin/settings"));
    sections.add(new SectionStatusItem("i18n", true, "/platform-admin/i18n"));
    sections.add(new SectionStatusItem("page_models", true, "/platform-admin/page-models"));
    sections.add(new SectionStatusItem("themes", true, "/platform-admin/themes"));
    return sections;
  }
}
