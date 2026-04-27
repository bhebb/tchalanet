package com.tchalanet.server.core.limitpolicy.application.command.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import jakarta.validation.constraints.NotNull;

public record UpsertLimitDefinitionCommand(
    @NotNull RuleKey ruleKey,
    boolean enabled,
    @NotNull BreachOutcome onBreach,
    @NotNull JsonNode params,
    @NotNull JsonNode appliesTo
) implements Command<UpsertLimitDefinitionResult> {}
