package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PromotionDecisionId(UUID value) {
  public PromotionDecisionId {
    if (value == null) {
      throw new IllegalArgumentException("PromotionDecisionId.value is null");
    }
  }

  public static PromotionDecisionId of(UUID value) { return new PromotionDecisionId(value); }
  public static PromotionDecisionId nullableOf(UUID raw) { return raw == null ? null : new PromotionDecisionId(raw); }
  public static PromotionDecisionId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("PromotionDecisionId string is required");
    return new PromotionDecisionId(UUID.fromString(raw));
  }
}
