package com.tchalanet.server.core.pricing.api.model;

import java.math.BigDecimal;

public record SellerTerminalPayoutRuleResolutionView(
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    PayoutRuleType tenantDefaultRuleType,
    BigDecimal tenantDefaultMultiplier,
    BigDecimal tenantDefaultFixedAmount,
    PayoutRuleType sellerTerminalRuleType,
    BigDecimal sellerTerminalMultiplier,
    BigDecimal sellerTerminalFixedAmount,
    PayoutRuleType effectiveRuleType,
    BigDecimal effectiveMultiplier,
    BigDecimal effectiveFixedAmount,
    OddsSource source) {}
