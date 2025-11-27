package com.tchalanet.server.limitpolicy.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Aggregate Root for a Limit Policy. Encapsulates rules for maximum stakes, payouts, etc. */
public class LimitPolicy {

  private final UUID id;
  private final UUID tenantId;
  private LimitScope scope;
  private String target; // e.g., gameCode, terminalId, userId, sessionId, selection
  private BigDecimal dailyCap;
  private BigDecimal maxStakePerLine;
  private BigDecimal maxPayoutPerLine;
  private BreachOutcome onBreach;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  // Private constructor for factory methods
  private LimitPolicy(
      UUID id,
      UUID tenantId,
      LimitScope scope,
      String target,
      BigDecimal dailyCap,
      BigDecimal maxStakePerLine,
      BigDecimal maxPayoutPerLine,
      BreachOutcome onBreach,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.scope = scope;
    this.target = target;
    this.dailyCap = dailyCap;
    this.maxStakePerLine = maxStakePerLine;
    this.maxPayoutPerLine = maxPayoutPerLine;
    this.onBreach = onBreach;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Factory method to create a new LimitPolicy. */
  public static LimitPolicy create(
      UUID tenantId,
      LimitScope scope,
      String target,
      BigDecimal dailyCap,
      BigDecimal maxStakePerLine,
      BigDecimal maxPayoutPerLine,
      BreachOutcome onBreach) {
    Objects.requireNonNull(tenantId, "TenantId cannot be null");
    Objects.requireNonNull(scope, "Scope cannot be null");
    Objects.requireNonNull(onBreach, "OnBreach cannot be null");
    // Further validations can be added here
    return new LimitPolicy(
        UUID.randomUUID(),
        tenantId,
        scope,
        target,
        dailyCap,
        maxStakePerLine,
        maxPayoutPerLine,
        onBreach,
        true,
        Instant.now(),
        Instant.now());
  }

  /** Factory method to load a LimitPolicy from persistence. */
  public static LimitPolicy load(
      UUID id,
      UUID tenantId,
      LimitScope scope,
      String target,
      BigDecimal dailyCap,
      BigDecimal maxStakePerLine,
      BigDecimal maxPayoutPerLine,
      BreachOutcome onBreach,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    return new LimitPolicy(
        id,
        tenantId,
        scope,
        target,
        dailyCap,
        maxStakePerLine,
        maxPayoutPerLine,
        onBreach,
        active,
        createdAt,
        updatedAt);
  }

  // --- Business Methods ---
  public void update(
      LimitScope scope,
      String target,
      BigDecimal dailyCap,
      BigDecimal maxStakePerLine,
      BigDecimal maxPayoutPerLine,
      BreachOutcome onBreach,
      boolean active) {
    this.scope = scope;
    this.target = target;
    this.dailyCap = dailyCap;
    this.maxStakePerLine = maxStakePerLine;
    this.maxPayoutPerLine = maxPayoutPerLine;
    this.onBreach = onBreach;
    this.active = active;
    this.updatedAt = Instant.now();
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public LimitScope getScope() {
    return scope;
  }

  public String getTarget() {
    return target;
  }

  public BigDecimal getDailyCap() {
    return dailyCap;
  }

  public BigDecimal getMaxStakePerLine() {
    return maxStakePerLine;
  }

  public BigDecimal getMaxPayoutPerLine() {
    return maxPayoutPerLine;
  }

  public BreachOutcome getOnBreach() {
    return onBreach;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
