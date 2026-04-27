package com.tchalanet.server.core.limitpolicy.application.query.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;

import java.time.Instant;
import java.util.List;

public record ListLimitAssignmentsView(
    LimitTarget target,
    List<Item> items
) {
  public record Item(
      LimitAssignmentId id,
      LimitDefinitionId limitDefinitionId,
      boolean enabled,
      Instant startsAt,
      Instant endsAt,
      JsonNode paramsOverride,
      JsonNode appliesToOverride
  ) {}
}
