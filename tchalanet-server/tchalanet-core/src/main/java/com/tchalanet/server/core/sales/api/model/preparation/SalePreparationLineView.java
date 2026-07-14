package com.tchalanet.server.core.sales.api.model.preparation;

import java.math.BigDecimal;

public record SalePreparationLineView(
    int lineNumber,
    String gameCode,
    String betType,
    Short betOption,
    String selection,
    BigDecimal stakeAmount,
    String origin) {}
