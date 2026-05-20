package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import tools.jackson.databind.JsonNode;

public record EffectiveLimitRule(
    RuleKey ruleKey,
    BreachOutcome onBreach,
    LimitScopeRef appliedScope,
    LimitAssignmentId assignmentId,
    JsonNode params
) {}
