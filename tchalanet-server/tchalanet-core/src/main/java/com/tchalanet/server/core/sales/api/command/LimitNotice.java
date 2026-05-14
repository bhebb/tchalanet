package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import java.math.BigDecimal;

public record LimitNotice(
    String ruleKey,
    BreachOutcome severity,
    String message,
    String targetApplied,
    String selectionKey,
    BigDecimal currentValue,
    BigDecimal limitValue) {}
