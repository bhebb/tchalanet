package com.tchalanet.server.catalog.plan.internal.cache;

/**
 * Cache names for catalog/plan.
 * Maps to spec requirement P5 (cache policy).
 */
public final class PlanCacheNames {

  private PlanCacheNames() {}

  public static final String ACTIVE_PLANS = "catalog:plan:active_plans";
  public static final String PLAN_BY_CODE = "catalog:plan:plan_by_code";
  public static final String PLAN_BY_ID = "catalog:plan:plan_by_id";
}
