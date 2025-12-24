package com.tchalanet.server.core.billing.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object for a Plan's unique identifier. */
public record PlanId(UUID value) {
  public PlanId {
    Objects.requireNonNull(value, "PlanId value cannot be null");
  }

  public static PlanId of(UUID value) {
    return new PlanId(value);
  }
}
