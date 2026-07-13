package com.tchalanet.server.core.pricing.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalPricingRuleOverrideView(
    SellerTerminalOddsOverrideId id,
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    PayoutRuleType payoutRuleType,
    BigDecimal fixedAmount,
    boolean active,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason,
    Instant createdAt,
    Instant updatedAt
) {}
