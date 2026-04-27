package com.tchalanet.server.core.limitpolicy.application.query.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitDefinitionId;

import java.util.List;

public record ListLimitDefinitionsView(
    List<Item> items
) {
  public record Item(
      LimitDefinitionId id,
      RuleKey ruleKey,
      boolean enabled,
      BreachOutcome onBreach,
      JsonNode params,
      JsonNode appliesTo
  ) {}
}
