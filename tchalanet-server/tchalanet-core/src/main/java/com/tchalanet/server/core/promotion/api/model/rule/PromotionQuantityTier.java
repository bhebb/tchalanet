package com.tchalanet.server.core.promotion.api.model.rule;

import java.math.BigDecimal;

public record PromotionQuantityTier(
    BigDecimal minPaidAmount,
    BigDecimal maxPaidAmount,
    int quantity
) {
}
