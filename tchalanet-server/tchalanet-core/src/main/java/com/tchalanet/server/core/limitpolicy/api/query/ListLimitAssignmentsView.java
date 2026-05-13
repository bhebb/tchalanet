package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record ListLimitAssignmentsView(
    LimitScopeQueryRef limitScopeRef,
    List<Item> items
) {

    public record Item(
        LimitAssignmentId id,
        RuleKey ruleKey,
        boolean enabled,
        BreachOutcome onBreach,
        JsonNode params,
        Instant startsAt,
        Instant endsAt
    ) {}
}
