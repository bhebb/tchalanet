package com.tchalanet.server.core.sales.api.event.payload;

import java.math.BigDecimal;

public record PromotionEffectPayload(
    String ruleId,
    String type,
    String gameCode,
    int quantity,
    BigDecimal amount,
    String currency,
    String appliesTo,
    String reason,
    String choiceMode
) {}
