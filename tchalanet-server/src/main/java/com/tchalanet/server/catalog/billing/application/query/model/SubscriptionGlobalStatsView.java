package com.tchalanet.server.catalog.billing.application.query.model;

import java.util.List;

/** Global statistics for subscriptions across all tenants. */
public record SubscriptionGlobalStatsView(
    int total, int active, int pastDue, int canceled, List<ByPlanItem> byPlan) {

  public record ByPlanItem(String planCode, int total, int active) {}
}
