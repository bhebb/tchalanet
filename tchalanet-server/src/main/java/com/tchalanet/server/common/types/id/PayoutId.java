package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Payout. */
public record PayoutId(UUID value) {

  public PayoutId {
    if (value == null) throw new IllegalArgumentException("PayoutId.value is null");
  }

  public static PayoutId of(UUID value) {
    return new PayoutId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static PayoutId nullableOf(UUID raw) {
    return raw == null ? null : new PayoutId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static PayoutId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("PayoutId string is required");
    }
    return new PayoutId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
