package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;

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
