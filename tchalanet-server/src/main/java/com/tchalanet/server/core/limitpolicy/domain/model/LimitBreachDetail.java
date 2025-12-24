package com.tchalanet.server.core.limitpolicy.domain.model;

import java.math.BigDecimal;

/**
 * Detailed information about a specific limit breach.
 *
 * Provides context about which rule was breached, the severity of the breach,
 * and the values involved in the violation.
 */
public record LimitBreachDetail(
    String ruleKey,
    BreachOutcome outcome,
    String message,
    TargetType targetApplied,
    String selectionKey,
    BigDecimal currentValue,
    BigDecimal limitValue
) {}
