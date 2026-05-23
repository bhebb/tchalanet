package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PromotionRuleId(UUID value) {
  public PromotionRuleId {
    if (value == null) throw new IllegalArgumentException("PromotionRuleId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static PromotionRuleId of(UUID value) {
    return new PromotionRuleId(value);
  }

  public static PromotionRuleId nullableOf(UUID value) {
    return value == null ? null : new PromotionRuleId(value);
  }

  public static PromotionRuleId parse(String value) {
    return value == null ? null : new PromotionRuleId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
