package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Plan. */
public record PlanId(UUID value) {

  public PlanId {
    if (value == null) throw new IllegalArgumentException("PlanId.value is null");
  }

  public static PlanId of(UUID value) {
    return new PlanId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static PlanId nullableOf(UUID raw) {
    return raw == null ? null : new PlanId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static PlanId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("PlanId string is required");
    }
    return new PlanId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
