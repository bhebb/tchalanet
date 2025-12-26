package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for Payout. */
public record PayoutId(UUID value) {

  public PayoutId {
    if (value == null) throw new IllegalArgumentException("PayoutId.value is null");
  }

  /** Static factory from UUID. */
  public static PayoutId of(UUID value) {
    return new PayoutId(value);
  }

  /** Return PayoutId or null if id is null */
  public static PayoutId nullableOf(UUID id) {
    return id == null ? null : new PayoutId(id);
  }

  /**
   * Static factory from String representation of UUID.
   *
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static PayoutId of(String id) {
    if (id == null) throw new IllegalArgumentException("payout id string is required");
    return new PayoutId(UUID.fromString(id));
  }

  public static PayoutId random() {
    return new PayoutId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() {
    return value;
  }
}
