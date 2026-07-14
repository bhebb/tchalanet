package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record ListLimitAssignmentsView(LimitScopeQueryRef limitScopeRef, List<Item> items) {

  public record Item(
      LimitAssignmentId id,
      RuleKey ruleKey,
      boolean enabled,
      BreachOutcome onBreach,
      JsonNode params,
      Instant startsAt,
      Instant endsAt) {}
}
