package com.tchalanet.server.core.pricing.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;

public record TenantPricingOddsView(
    TenantId tenantId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active
) {}
