package com.tchalanet.server.core.promotion.api.model;

import java.math.BigDecimal;

public record FreeLineGrant(
    String ruleCode,
    int ruleVersion,
    String gameCode,
    int quantity,
    BigDecimal effectiveStakeAmount,
    boolean requiresUserSelection
) {}
