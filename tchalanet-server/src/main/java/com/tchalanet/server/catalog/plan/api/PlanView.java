package com.tchalanet.server.catalog.plan.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.PlanId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable view for Plan catalog (reference data).
 * Maps to spec requirement P4 (mapping boundaries).
 * Exposed by PlanCatalog API (catalog/plan/api).
 */
public record PlanView(
    PlanId id,
    String code,
    String name,
    String description,
    BigDecimal priceAmount,
    String currency,
    String billingPeriod,
    JsonNode limitsJson,
    JsonNode featuresJson,
    boolean active,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt
) {}
