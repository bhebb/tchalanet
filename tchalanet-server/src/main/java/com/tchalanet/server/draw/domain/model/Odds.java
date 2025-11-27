package com.tchalanet.server.draw.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the odds/multiplier for a specific game selection. This is an immutable Value Object.
 */
public record Odds(
    UUID id,
    UUID tenantId,
    String gameCode,
    BigDecimal multiplier,
    Instant validFrom,
    Instant validTo) {

  public Odds {
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(tenantId, "TenantId cannot be null");
    Objects.requireNonNull(gameCode, "GameCode cannot be null");
    Objects.requireNonNull(multiplier, "Multiplier cannot be null");
    Objects.requireNonNull(validFrom, "ValidFrom cannot be null");

    if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Multiplier must be positive.");
    }
    if (validTo != null && validFrom.isAfter(validTo)) {
      throw new IllegalArgumentException("ValidFrom cannot be after ValidTo.");
    }
  }

  /**
   * Checks if these odds are valid at a given instant.
   *
   * @param time The instant to check against.
   * @return true if the odds are valid, false otherwise.
   */
  public boolean isValidAt(Instant time) {
    Objects.requireNonNull(time, "Time cannot be null");
    return !time.isBefore(validFrom) && (validTo == null || !time.isAfter(validTo));
  }

  /**
   * Applies the multiplier to a given stake to calculate potential payout.
   *
   * @param stake The amount to multiply.
   * @return The potential payout.
   */
  public BigDecimal applyToStake(BigDecimal stake) {
    Objects.requireNonNull(stake, "Stake cannot be null");
    if (stake.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Stake cannot be negative.");
    }
    return stake.multiply(multiplier);
  }
}
