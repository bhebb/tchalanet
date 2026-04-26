package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PlanId(UUID value) {
  public PlanId {
    if (value == null) throw new IllegalArgumentException("PlanId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static PlanId of(UUID value) {
    return new PlanId(value);
  }

  public static PlanId nullableOf(UUID value) {
    return value == null ? null : new PlanId(value);
  }

  public static PlanId parse(String value) {
    return value == null ? null : new PlanId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
