package com.tchalanet.server.catalog.plan.internal.cache;

/**
 * Cache names for catalog/plan.
 * Maps to spec requirement P5 (cache policy).
 */
public final class PlanCacheNames {

  private PlanCacheNames() {}

  public static final String ACTIVE_PLANS = "catalog.plan.cache.ACTIVE_PLANS";
  public static final String PLAN_BY_CODE = "catalog.plan.cache.PLAN_BY_CODE";
  public static final String PLAN_BY_ID = "catalog.plan.cache.PLAN_BY_ID";
}
