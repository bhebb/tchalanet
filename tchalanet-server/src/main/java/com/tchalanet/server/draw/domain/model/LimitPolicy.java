package com.tchalanet.server.draw.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Represents a limit policy for a specific scope and target. This is an immutable Value Object. */
public record LimitPolicy(
    UUID id,
    UUID tenantId,
    String scope, // e.g., "GLOBAL", "GAME", "TERMINAL"
    String target, // e.g., gameCode, terminalId
    BigDecimal dailyCap,
    LimitPolicy.BreachOutcome onBreach // Using an enum for clarity and type-safety
    ) {
  public LimitPolicy {
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(tenantId, "TenantId cannot be null");
    Objects.requireNonNull(scope, "Scope cannot be null");
    Objects.requireNonNull(target, "Target cannot be null");
    Objects.requireNonNull(dailyCap, "DailyCap cannot be null");
    Objects.requireNonNull(onBreach, "OnBreach cannot be null");

    if (dailyCap.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("DailyCap cannot be negative.");
    }
  }

  /** Defines the action to take when a limit is breached. */
  public enum BreachOutcome {
    BLOCK, // Refuse the action
    WARN, // Allow the action but log a warning
    ALLOW // Allow the action without warning (effectively no limit)
  }
}
