package com.tchalanet.server.catalog.pricing.internal.web.model;

import com.tchalanet.server.common.types.id.PricingOddsId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.enums.BetType;
import java.math.BigDecimal;

public record PricingOddsView(
    PricingOddsId id,
    TenantId tenantId,
    String gameCode,
    BetType betType,
    Short betOption,
    BigDecimal odds,
    boolean active
) {}
