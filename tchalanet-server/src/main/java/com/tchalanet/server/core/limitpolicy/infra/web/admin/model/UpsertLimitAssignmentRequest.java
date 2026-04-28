package com.tchalanet.server.core.limitpolicy.infra.web.admin.model;

import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record UpsertLimitAssignmentRequest(
    @NotNull LimitDefinitionId limitDefinitionId,
    @NotNull LimitTarget target,
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    JsonNode paramsOverride,
    JsonNode appliesToOverride
) {}
