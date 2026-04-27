package com.tchalanet.server.core.subscription.application.query.model;

import java.util.List;

public record PlatformSubscriptionStatsView(
    int total,
    int active,
    int pastDue,
    int canceled,
    List<ByPlanRow> byPlan
) {
  public record ByPlanRow(String planCode, int total, int active) {}
}
