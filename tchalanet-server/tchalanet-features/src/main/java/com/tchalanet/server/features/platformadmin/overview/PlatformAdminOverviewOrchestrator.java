package com.tchalanet.server.features.platformadmin.overview;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantStatsView;
import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static com.tchalanet.server.features.platformadmin.overview.PlatformAdminOverviewView.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAdminOverviewOrchestrator {

    private final Clock clock;
    private final QueryBus queryBus;

    // Catalogs
    private final TenantPreContextLookupApi tenantCatalog; // ✅ direct
    private final GameCatalog gameCatalog;
    private final ResultSlotCatalog resultSlotCatalog;
    private final PageModelTemplateCatalog pageModelTemplateCatalog;
    private final ThemeCatalog themeCatalog;
    private final PricingCatalog pricingCatalog;
    private final I18nOverridesCatalog i18nGlobalCatalog;
    private final SettingsCatalog settingsCatalog;

    private final PlatformAdminOverviewSections sections;

    public PlatformAdminOverviewView overview() {
        Instant now = Instant.now(clock);

        // --- Tenants: catalog direct
        TenantStatsView tenants = tenantCatalog.stats();

        // --- Catalog referentials
        var games = gameCatalog.stats();
        var slots = resultSlotCatalog.stats();
        var templates = pageModelTemplateCatalog.stats();
        var themes = themeCatalog.stats();
        var i18nKeys = i18nGlobalCatalog.keyStats();

        CatalogStats catalog = new CatalogStats(
            new CountItem(games.total(), games.active()),
            new CountItem(slots.total(), slots.active()),
            new CountItem(i18nKeys.totalKeys(), i18nKeys.totalKeys()), // represent as total/active duplication for now
            new CountItem(templates.total(), templates.active()),
            new CountItem(themes.total(), themes.active())
        );

        // --- Subscriptions: core via QueryBus
        PlatformSubscriptionStatsView subs = queryBus.ask(new GetPlatformSubscriptionStatsQuery());

        CoreStats core = new CoreStats(
            new TenantStats(tenants.total(), tenants.active(), tenants.suspended()),
            new SubscriptionStats(
                subs == null ? 0 : subs.total(),
                subs == null ? 0 : subs.active(),
                subs == null ? 0 : subs.pastDue(),
                subs == null ? 0 : subs.canceled(),
                subs == null ? List.of() : subs.byPlan().stream()
                    .map(p -> new SubscriptionStats.ByPlanItem(p.planCode(), p.total(), p.active()))
                    .toList()
            )
        );

        // --- Platform stats: pricing
        var pricing = pricingCatalog.stats();
        var settingsStats = settingsCatalog.stats();
        PlatformStats platform = new PlatformStats(
            new CountItem(0, 0), // plans placeholder
            new CountItem(pricing.total(), pricing.active()),
            new CountItem(settingsStats.totalGlobalSettings(), settingsStats.totalActiveSettings())
        );

        List<SectionStatusItem> sectionItems = sections.items();

        return new PlatformAdminOverviewView(now, catalog, core, platform, sectionItems);
    }
}
