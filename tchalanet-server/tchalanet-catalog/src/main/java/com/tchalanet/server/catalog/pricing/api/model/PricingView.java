package com.tchalanet.server.catalog.pricing.api.model;

import com.tchalanet.server.common.types.id.PricingOddsId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;

/** Read-only view of a pricing odds entry */
public record PricingView(
    PricingOddsId id,
    TenantId tenantId,
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
