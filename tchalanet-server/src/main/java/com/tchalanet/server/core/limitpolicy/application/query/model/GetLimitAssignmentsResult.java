package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GetLimitAssignmentsResult(List<GetLimitAssignmentsResult.AssignmentSummary> assignments) {

    public record AssignmentSummary(
        UUID assignmentId,
        UUID limitDefinitionId,
        RuleKey ruleKey,
        boolean limitEnabled,
        BreachOutcome onBreach,
        Map<String, Object> params,
        boolean assignmentEnabled,
        Instant startsAt,
        Instant endsAt
    ) {
    }
}
