package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BetType;

import java.math.BigDecimal;
import java.util.Map;

public record LimitFactsSnapshot(Map<Key, Fact> bySelection) {
  public record Key(BetType betType, String selectionKey) {}
  public record Fact(BigDecimal stakeTotal, BigDecimal potentialPayoutTotal, long salesCount) {}

  public static final LimitFactsSnapshot EMPTY = new LimitFactsSnapshot(Map.of());
}
