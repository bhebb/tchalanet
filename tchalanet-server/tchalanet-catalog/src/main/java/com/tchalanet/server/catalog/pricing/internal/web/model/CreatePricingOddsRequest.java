package com.tchalanet.server.catalog.pricing.internal.web.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.enums.BetType;

public record CreatePricingOddsRequest(
    TenantId tenantId,
    String gameCode,
    BetType betType,
    Short betOption,
    java.math.BigDecimal odds,
    Boolean active) implements BasePricingOddsRequest {}
