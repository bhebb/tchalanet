package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;

public record LimitBreachView(
    RuleKey ruleKey,
    BreachOutcome outcome,
    String appliedScope,
    String code,
    String messageKey,
    Long limitValue,
    Long currentValue,
    Long deltaValue
) {
}
