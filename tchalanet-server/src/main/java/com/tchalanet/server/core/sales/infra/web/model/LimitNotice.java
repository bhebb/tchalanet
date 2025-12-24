package com.tchalanet.server.core.sales.infra.web.model;

import java.math.BigDecimal;

public record LimitNotice(
    String ruleKey,
    String message,
    String targetApplied,
    String selectionKey,
    BigDecimal currentValue,
    BigDecimal limitValue
) {
}
