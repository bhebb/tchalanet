package com.tchalanet.server.core.promotion.api.model;

import java.math.BigDecimal;

public record PayoutModifier(
    String ruleCode,
    int ruleVersion,
    PromotionEffectType effectType,
    String gameCode,
    PrizeRank prizeRank,
    BigDecimal baseMultiplier,
    BigDecimal appliedMultiplier,
    BigDecimal boostMultiplier
) {}
