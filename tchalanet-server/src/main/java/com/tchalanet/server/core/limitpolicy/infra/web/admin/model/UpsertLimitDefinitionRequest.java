package com.tchalanet.server.core.limitpolicy.infra.web.admin.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record UpsertLimitDefinitionRequest(
    @NotNull RuleKey ruleKey,
    boolean enabled,
    @NotNull BreachOutcome onBreach,
    @NotNull JsonNode params,
    @NotNull JsonNode appliesTo
) {}
