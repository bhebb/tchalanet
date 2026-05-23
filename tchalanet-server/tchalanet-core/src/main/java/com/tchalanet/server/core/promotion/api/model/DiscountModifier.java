package com.tchalanet.server.core.promotion.api.model;

import java.math.BigDecimal;

public record DiscountModifier(
    String ruleCode,
    int ruleVersion,
    PromotionEffectType effectType,
    BigDecimal amount,
    BigDecimal percent
) {}
