package com.tchalanet.server.core.billing.domain.model;

import com.tchalanet.server.common.types.id.PlanId;
import java.math.BigDecimal;
import java.util.Map;

public record Plan(
    PlanId id,
    String code,
    String name,
    String description,
    BigDecimal priceAmount,
    String currency,
    BillingFrequency billingFrequency,
    boolean publicPlan,
    Map<String, Object> features) {}
