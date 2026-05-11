package com.tchalanet.server.core.limitpolicy.application.query.model.assignment;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record ListLimitAssignmentsView(
    LimitScopeRef limitScopeRef,
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
