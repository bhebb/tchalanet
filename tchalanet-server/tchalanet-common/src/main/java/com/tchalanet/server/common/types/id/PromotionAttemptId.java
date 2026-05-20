package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PromotionAttemptId(UUID value) {

  public PromotionAttemptId {
    if (value == null) {
      throw new IllegalArgumentException("PromotionAttemptId.value is null");
    }
  }

  public static PromotionAttemptId of(UUID value) {
    return new PromotionAttemptId(value);
  }

  public static PromotionAttemptId nullableOf(UUID value) {
    return value == null ? null : new PromotionAttemptId(value);
  }

  public static PromotionAttemptId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("PromotionAttemptId string is required");
    }
    return new PromotionAttemptId(UUID.fromString(raw));
  }

  public UUID uuid() {
    return value;
  }
}
