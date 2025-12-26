package com.tchalanet.server.core.limitpolicy.domain.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LimitDefinition(
    UUID id,
    TenantId tenantId,
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
