package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PromotionRuleId(UUID value) {
  public PromotionRuleId {
    if (value == null) {
      throw new IllegalArgumentException("PromotionRuleId.value is null");
    }
  }

  public static PromotionRuleId of(UUID value) { return new PromotionRuleId(value); }
  public static PromotionRuleId nullableOf(UUID raw) { return raw == null ? null : new PromotionRuleId(raw); }
  public static PromotionRuleId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("PromotionRuleId string is required");
    return new PromotionRuleId(UUID.fromString(raw));
  }
}
