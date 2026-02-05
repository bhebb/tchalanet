package com.tchalanet.server.features.platformadmin.overview;

import java.time.Instant;
import java.util.List;

/** Platform Admin console overview view with catalog, core, and platform stats. */
public record PlatformAdminOverviewView(
    Instant generatedAt,
    CatalogStats catalog,
    CoreStats core,
    PlatformStats platform,
    List<SectionStatusItem> sections) {

  public record CatalogStats(
      CountItem games,
      CountItem resultSlots,
      CountItem i18nGlobalKeys,
      CountItem pageModelTemplates,
      CountItem themePresets) {}

  public record CoreStats(TenantStats tenants, SubscriptionStats subscriptions) {}

  public record TenantStats(int total, int active, int suspended) {}

  public record SubscriptionStats(
      int total, int active, int pastDue, int canceled, List<ByPlanItem> byPlan) {
    public record ByPlanItem(String planCode, int total, int active) {}
  }

  public record PlatformStats(CountItem plans, CountItem pricingRules, CountItem globalSettings) {}

  public record CountItem(int total, int active) {}

  public record SectionStatusItem(String key, boolean enabled, String href) {}
}
