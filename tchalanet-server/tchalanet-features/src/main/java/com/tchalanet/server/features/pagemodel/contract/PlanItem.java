package com.tchalanet.server.features.pagemodel.contract;

import java.util.Map;

/**
 * Typed contract for a subscription plan in the public home PlansWidget.
 * {@code features} is allowed as a free-form map (it is a JSON blob from the plan catalog).
 */
public record PlanItem(
    String value,
    String name,
    String description,
    Object price,
    String currency,
    String billingPeriod,
    Map<String, Object> features,
    boolean isDefault) {}
