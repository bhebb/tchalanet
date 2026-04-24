package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.RuleKey;

import java.math.BigDecimal;
import java.util.Map;

public record EffectiveLimits(
    Map<RuleKey, BigDecimal> values
) {
  public BigDecimal get(RuleKey key) { return values.get(key); }
}
