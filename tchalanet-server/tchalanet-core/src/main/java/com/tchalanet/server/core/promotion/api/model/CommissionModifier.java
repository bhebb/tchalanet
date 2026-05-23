package com.tchalanet.server.core.promotion.api.model;

import java.math.BigDecimal;

public record CommissionModifier(
    String ruleCode,
    int ruleVersion,
    BigDecimal percentBoost
) {}
