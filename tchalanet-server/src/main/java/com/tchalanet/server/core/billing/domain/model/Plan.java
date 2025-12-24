package com.tchalanet.server.core.billing.domain.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record Plan(
    UUID id,
    String code,
    String name,
    String description,
    BigDecimal priceAmount,
    String currency,
    BillingFrequency billingFrequency,
    boolean publicPlan,
    Map<String, Object> features) {}
