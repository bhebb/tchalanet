package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;

public record LimitBreach(
    RuleKey ruleKey,
    BreachOutcome outcome,
    LimitScopeRef appliedScope,
    String code,
    String messageKey,
    Long limitValue,
    Long currentValue,
    Long deltaValue
) {}
