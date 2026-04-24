package com.tchalanet.server.features.tenantadmin.policies.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import jakarta.validation.constraints.NotNull;

/** Feature-level DTO for creating/updating a LimitDefinition. */
public record UpsertLimitDefinitionRequest(
    @NotNull RuleKey ruleKey,
    boolean enabled,
    @NotNull BreachOutcome onBreach,
    @NotNull JsonNode params,
    @NotNull JsonNode appliesTo
) {}
