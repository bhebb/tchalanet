package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpsertLimitAssignmentCommand(
    @NotNull LimitDefinitionId limitDefinitionId,
    @NotNull LimitTarget target,
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    JsonNode paramsOverride,      // nullable => no override
    JsonNode appliesToOverride    // nullable => no override
) implements Command<com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitAssignmentResult> {}
