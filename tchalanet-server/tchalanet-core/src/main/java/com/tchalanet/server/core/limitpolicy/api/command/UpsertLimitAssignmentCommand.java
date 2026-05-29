package com.tchalanet.server.core.limitpolicy.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record UpsertLimitAssignmentCommand(
    TenantId tenantId,
    RuleKey ruleKey,
    LimitScopeRef target,
    boolean enabled,
    BreachOutcome onBreach,
    JsonNode params,
    Instant startsAt,
    Instant endsAt
) implements Command<UpsertLimitAssignmentResult> {
}
