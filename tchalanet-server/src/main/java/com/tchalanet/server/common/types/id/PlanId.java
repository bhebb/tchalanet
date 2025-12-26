package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value Object for a Plan's unique identifier. */
public record PlanId(UUID value) {
  public PlanId {
    if (value == null) throw new IllegalArgumentException("PlanId.value is null");
  }

  public static PlanId of(UUID value) {
    return new PlanId(value);
  }

  /** Return PlanId or null if id is null */
  public static PlanId nullableOf(UUID id) { return id == null ? null : new PlanId(id); }

  public static PlanId of(String id) {
    if (id == null) throw new IllegalArgumentException("plan id string is required");
    return new PlanId(UUID.fromString(id));
  }

  public static PlanId random() {
    return new PlanId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() { return value; }
}
