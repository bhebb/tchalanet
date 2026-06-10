package com.tchalanet.server.core.sales.api.model.preparation;

import java.math.BigDecimal;

public record SalePreparationPromotionLineView(
    String lineRef,
    String gameCode,
    String betType,
    Short betOption,
    String selection,
    BigDecimal payoutBaseAmount,
    boolean regenerable,
    int regenerationsRemaining
) {}
