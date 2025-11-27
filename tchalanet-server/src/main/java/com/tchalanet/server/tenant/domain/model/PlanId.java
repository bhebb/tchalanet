package com.tchalanet.server.tenant.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object for a Plan's unique identifier. */
public record PlanId(UUID value) {
  public PlanId {
    Objects.requireNonNull(value, "PlanId value cannot be null");
  }
}
