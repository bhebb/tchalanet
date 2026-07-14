package com.tchalanet.server.catalog.plan.api;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.PlanId;
import java.math.BigDecimal;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Immutable view for Plan catalog (reference data). Maps to spec requirement P4 (mapping
 * boundaries). Exposed by PlanCatalog API (catalog/plan/api).
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
    Instant updatedAt) {
  // Normalize null -> empty object for consistent L2 cache round-trip (null JsonNode -> NullNode).
  public PlanView {
    limitsJson = limitsJson != null ? limitsJson : JsonUtils.emptyObject();
    featuresJson = featuresJson != null ? featuresJson : JsonUtils.emptyObject();
  }
}
