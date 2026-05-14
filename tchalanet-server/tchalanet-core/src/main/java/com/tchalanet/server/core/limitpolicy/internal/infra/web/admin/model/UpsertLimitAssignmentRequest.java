package com.tchalanet.server.core.limitpolicy.internal.infra.web.admin.model;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.TargetType;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record UpsertLimitAssignmentRequest(
    @NotNull RuleKey ruleKey,
    @NotNull TargetType targetType,
    String targetId,
    boolean enabled,
    @NotNull BreachOutcome onBreach,
    @NotNull JsonNode params,
    Instant startsAt,
    Instant endsAt
) {}
