package com.tchalanet.server.features.tenantadmin.policies.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

/** Feature-level DTO for creating/updating a LimitDefinition. */
public record UpsertLimitDefinitionRequest(
    @NotNull RuleKey ruleKey,
    boolean enabled,
    @NotNull BreachOutcome onBreach,
    @NotNull JsonNode params,
    @NotNull JsonNode appliesTo
) {}
