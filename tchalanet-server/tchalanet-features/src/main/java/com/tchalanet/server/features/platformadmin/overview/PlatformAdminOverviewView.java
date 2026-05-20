package com.tchalanet.server.features.platformadmin.overview;

import java.time.Instant;
import java.util.List;

public record PlatformAdminOverviewView(
    Instant generatedAt,
    CatalogStats catalog,
    CoreStats core,
    PlatformStats platform,
    List<SectionStatusItem> sections
) {
    // --- Catalog = referentiels globaux (pas lifecycle)
    public record CatalogStats(
        CountItem games,
        CountItem resultSlots,
        CountItem i18nGlobalKeys,
        CountItem pageModelTemplates,
        CountItem themePresets
    ) {}

    // --- Core = etat metier (lifecycle)
    public record CoreStats(
        TenantStats tenants,
        SubscriptionStats subscriptions
    ) {}

    public record TenantStats(int total, int active, int suspended) {}

    public record SubscriptionStats(
        int total,
        int active,
        int pastDue,
        int canceled,
        List<ByPlanItem> byPlan
    ) {
        public record ByPlanItem(String planCode, int total, int active) {}
    }

    // --- Platform = config globale non-catalog (billing/settings)
    public record PlatformStats(
        CountItem plans,
        CountItem pricingRules,
        CountItem globalSettings
    ) {}

    public record CountItem(int total, int active) {}

    public record SectionStatusItem(String key, boolean enabled, String href) {}
}
