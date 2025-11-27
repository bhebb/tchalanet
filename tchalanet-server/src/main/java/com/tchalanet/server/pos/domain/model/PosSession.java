package com.tchalanet.server.pos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for a Point-of-Sale (POS) Session. Encapsulates the state and business rules for a
 * session.
 */
public class PosSession {

  private final UUID id;
  private final UUID tenantId;
  private final UUID terminalId;
  private final UUID userId;
  private PosSessionStatus status;
  private final Instant openedAt;
  private Instant closedAt;
  private Instant lastActivityAt;
  private BigDecimal openingFloat; // Optional: initial cash in drawer
  private BigDecimal closingAmount; // Optional: final cash in drawer
  private BigDecimal totalTicketsAmount; // Aggregated from tickets
  private BigDecimal totalPayoutAmount; // Aggregated from tickets
  private BigDecimal grossMargin; // totalTicketsAmount - totalPayoutAmount

  // Private constructor to enforce creation via factory methods
  private PosSession(
      UUID id,
      UUID tenantId,
      UUID terminalId,
      UUID userId,
      Instant openedAt,
      BigDecimal openingFloat) {
    this.id = id;
    this.tenantId = tenantId;
    this.terminalId = terminalId;
    this.userId = userId;
    this.status = PosSessionStatus.OPEN;
    this.openedAt = openedAt;
    this.lastActivityAt = openedAt;
    this.openingFloat = openingFloat;
    this.totalTicketsAmount = BigDecimal.ZERO;
    this.totalPayoutAmount = BigDecimal.ZERO;
    this.grossMargin = BigDecimal.ZERO;
  }

  // Constructor for loading from persistence
  private PosSession(
      UUID id,
      UUID tenantId,
      UUID terminalId,
      UUID userId,
      PosSessionStatus status,
      Instant openedAt,
      Instant closedAt,
      Instant lastActivityAt,
      BigDecimal openingFloat,
      BigDecimal closingAmount,
      BigDecimal totalTicketsAmount,
      BigDecimal totalPayoutAmount,
      BigDecimal grossMargin) {
    this.id = id;
    this.tenantId = tenantId;
    this.terminalId = terminalId;
    this.userId = userId;
    this.status = status;
    this.openedAt = openedAt;
    this.closedAt = closedAt;
    this.lastActivityAt = lastActivityAt;
    this.openingFloat = openingFloat;
    this.closingAmount = closingAmount;
    this.totalTicketsAmount = totalTicketsAmount;
    this.totalPayoutAmount = totalPayoutAmount;
    this.grossMargin = grossMargin;
  }

  /** Factory method to create a new open POS session. */
  public static PosSession open(
      UUID tenantId, UUID terminalId, UUID userId, BigDecimal openingFloat) {
    Objects.requireNonNull(tenantId, "TenantId cannot be null");
    Objects.requireNonNull(terminalId, "TerminalId cannot be null");
    Objects.requireNonNull(userId, "UserId cannot be null");
    return new PosSession(
        UUID.randomUUID(), tenantId, terminalId, userId, Instant.now(), openingFloat);
  }

  /** Factory method to reconstruct a PosSession from persisted state. */
  public static PosSession load(
      UUID id,
      UUID tenantId,
      UUID terminalId,
      UUID userId,
      PosSessionStatus status,
      Instant openedAt,
      Instant closedAt,
      Instant lastActivityAt,
      BigDecimal openingFloat,
      BigDecimal closingAmount,
      BigDecimal totalTicketsAmount,
      BigDecimal totalPayoutAmount,
      BigDecimal grossMargin) {
    return new PosSession(
        id,
        tenantId,
        terminalId,
        userId,
        status,
        openedAt,
        closedAt,
        lastActivityAt,
        openingFloat,
        closingAmount,
        totalTicketsAmount,
        totalPayoutAmount,
        grossMargin);
  }

  // --- Business Methods ---

  public void close(BigDecimal closingAmount) {
    if (this.status != PosSessionStatus.OPEN) {
      throw new IllegalStateException(
          "Only an OPEN session can be closed. Current status: " + this.status);
    }
    this.status = PosSessionStatus.CLOSED;
    this.closedAt = Instant.now();
    this.closingAmount = closingAmount;
    // Aggregation of ticket amounts would happen here or be passed in.
  }

  public void autoClose() {
    if (this.status != PosSessionStatus.OPEN) {
      throw new IllegalStateException(
          "Only an OPEN session can be auto-closed. Current status: " + this.status);
    }
    this.status = PosSessionStatus.AUTO_CLOSED;
    this.closedAt = Instant.now();
    // Aggregation of ticket amounts would happen here or be passed in.
  }

  public void recordActivity(Instant activityTime) {
    if (this.status == PosSessionStatus.OPEN) {
      this.lastActivityAt = activityTime;
    }
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getTerminalId() {
    return terminalId;
  }

  public UUID getUserId() {
    return userId;
  }

  public PosSessionStatus getStatus() {
    return status;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public Instant getLastActivityAt() {
    return lastActivityAt;
  }

  public BigDecimal getOpeningFloat() {
    return openingFloat;
  }

  public BigDecimal getClosingAmount() {
    return closingAmount;
  }

  public BigDecimal getTotalTicketsAmount() {
    return totalTicketsAmount;
  }

  public BigDecimal getTotalPayoutAmount() {
    return totalPayoutAmount;
  }

  public BigDecimal getGrossMargin() {
    return grossMargin;
  }
}
