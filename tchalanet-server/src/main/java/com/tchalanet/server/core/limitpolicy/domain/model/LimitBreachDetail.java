package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
// Target applied is represented as string for easier mapping to API models
import java.math.BigDecimal;

/**
 * Detailed information about a specific limit breach.
 *
 * <p>Provides context about which rule was breached, the severity of the breach, and the values
 * involved in the violation.
 */
public record LimitBreachDetail(
    String ruleKey,
    BreachOutcome outcome,
    String message,
    String targetApplied,
    String selectionKey,
    BigDecimal currentValue,
    BigDecimal limitValue) {}
