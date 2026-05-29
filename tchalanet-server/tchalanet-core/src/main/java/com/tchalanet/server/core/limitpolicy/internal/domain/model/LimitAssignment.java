package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record LimitAssignment(LimitAssignmentId id,
                              RuleKey ruleKey,
                              LimitScopeRef scope,
                              boolean enabled,
                              BreachOutcome onBreach,
                              JsonNode params,
                              Instant startsAt,
                              Instant endsAt,
                              boolean deleted
) {

    public boolean isActiveAt(Instant now) {
        if (!enabled || deleted) {
            return false;
        }

        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }

        return endsAt == null || now.isBefore(endsAt);
    }

    public static LimitAssignment createNew(
        LimitAssignmentId id,
        RuleKey ruleKey,
        LimitScopeRef scopeRef,
        boolean enabled,
        BreachOutcome onBreach,
        JsonNode params,
        Instant startsAt,
        Instant endsAt
    ) {
        return new LimitAssignment(
            id,
            ruleKey,
            scopeRef,
            enabled,
            onBreach,
            params,
            startsAt,
            endsAt,
            false);
    }

    public LimitAssignment update(
        boolean enabled,
        BreachOutcome onBreach,
        JsonNode params,
        Instant startsAt,
        Instant endsAt
    ) {
        return new LimitAssignment(
            id,
            ruleKey,
            scope,
            enabled,
            onBreach,
            params,
            startsAt,
            endsAt,
            deleted);
    }

}
