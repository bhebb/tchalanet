package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;

import java.time.Instant;
import java.util.List;

public record LimitsOverviewView(
    LimitTarget target,
    List<Definition> definitions,
    List<Assignment> assignments,
    List<Effective> effective // computed (optional but very useful)
) {
  public record Definition(
      LimitDefinitionId id,
      RuleKey ruleKey,
      boolean enabled,
      BreachOutcome onBreach,
      JsonNode params,
      JsonNode appliesTo
  ) {}

  public record Assignment(
      LimitAssignmentId id,
      LimitDefinitionId limitDefinitionId,
      boolean enabled,
      Instant startsAt,
      Instant endsAt,
      JsonNode paramsOverride,
      JsonNode appliesToOverride
  ) {}

  public record Effective(
      RuleKey ruleKey,
      boolean enabled,               // definition.enabled AND assignment.enabled
      BreachOutcome onBreach,        // from definition
      JsonNode params,               // merged (override wins)
      JsonNode appliesTo,            // merged (override wins)
      LimitDefinitionId definitionId,
      LimitAssignmentId assignmentId // nullable if not assigned
  ) {}
}
