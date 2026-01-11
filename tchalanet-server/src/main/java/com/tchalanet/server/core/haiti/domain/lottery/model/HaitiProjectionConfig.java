package com.tchalanet.server.core.haiti.domain.lottery.model;

import java.util.Map;
import java.util.Objects;

public record HaitiProjectionConfig(Map<HaitiLot, HaitiProjectionToken> tokens) {
  public HaitiProjectionConfig {
    Objects.requireNonNull(tokens, "tokens");
    for (HaitiLot lot : HaitiLot.values())
      if (!tokens.containsKey(lot)) throw new IllegalArgumentException("Missing token for " + lot);
    tokens = Map.copyOf(tokens);
  }
}
