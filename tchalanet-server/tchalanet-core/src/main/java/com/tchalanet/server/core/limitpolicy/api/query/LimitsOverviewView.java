package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record LimitsOverviewView(
    LimitScopeRef limitScopeRef,
    List<Assignment> assignments
) {

    public record Assignment(
        LimitAssignmentId id,
        RuleKey ruleKey,
        boolean enabled,
        BreachOutcome onBreach,
        JsonNode params,
        Instant startsAt,
        Instant endsAt
    ) {}
}
