package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import java.math.BigDecimal;

public record LimitNotice(
    String ruleKey,
    BreachOutcome severity,
    String message,
    String targetApplied,
    String selectionKey,
    BigDecimal currentValue,
    BigDecimal limitValue) {}
