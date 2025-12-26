package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value Object for a Subscription's unique identifier. */
public record SubscriptionId(UUID value) {
  public SubscriptionId {
    if (value == null) throw new IllegalArgumentException("SubscriptionId.value is null");
  }

  /** Static factory from UUID. */
  public static SubscriptionId of(UUID value) {
    return new SubscriptionId(value);
  }

  /** Return SubscriptionId or null if id is null */
  public static SubscriptionId nullableOf(UUID id) {
    return id == null ? null : new SubscriptionId(id);
  }

  /** Static factory from String representation of UUID. */
  public static SubscriptionId of(String id) {
    if (id == null) throw new IllegalArgumentException("subscription id string is required");
    return new SubscriptionId(UUID.fromString(id));
  }

  public static SubscriptionId random() {
    return new SubscriptionId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() {
    return value;
  }
}
