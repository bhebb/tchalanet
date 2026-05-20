package com.tchalanet.server.catalog.pricing.internal.web.model;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.TenantId;

public record UpdatePricingOddsRequest(
    TenantId tenantId,
    String gameCode,
    BetType betType,
    Short betOption,
    java.math.BigDecimal odds,
    Boolean active) implements BasePricingOddsRequest {}
