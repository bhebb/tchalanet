package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record LimitDefinition(
    LimitDefinitionId id,
    RuleKey ruleKey,
    boolean enabled,
    BreachOutcome onBreach,
    JsonNode params,
    JsonNode appliesTo,
    Instant deletedAt) {

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
