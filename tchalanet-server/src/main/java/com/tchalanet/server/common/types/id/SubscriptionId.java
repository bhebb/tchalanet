package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Subscription. */
public record SubscriptionId(UUID value) {

  public SubscriptionId {
    if (value == null) throw new IllegalArgumentException("SubscriptionId.value is null");
  }

  public static SubscriptionId of(UUID value) {
    return new SubscriptionId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static SubscriptionId nullableOf(UUID raw) {
    return raw == null ? null : new SubscriptionId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static SubscriptionId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("SubscriptionId string is required");
    }
    return new SubscriptionId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
