package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;

import java.math.BigDecimal;

public record LimitBreachView(
    RuleKey ruleKey,
    BreachOutcome outcome,
    String appliedTarget,
    String code,
    String messageKey,
    BigDecimal limitValue,
    BigDecimal currentValue,
    BigDecimal deltaValue
) {}
