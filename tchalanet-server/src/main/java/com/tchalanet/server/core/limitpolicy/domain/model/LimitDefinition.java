package com.tchalanet.server.core.limitpolicy.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LimitDefinition(
    UUID id,
    UUID tenantId,
    RuleKey ruleKey,
    boolean enabled,
    BreachOutcome onBreach,
    Map<String, Object> params,
    AppliesTo appliesTo,
    long version
) {
  public record AppliesTo(
      java.util.List<String> betTypes,
      String selectionPattern
  ) {}
}
