package com.tchalanet.server.core.payout.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Aggregate root for a Payout. */
public class Payout {
  private final UUID id;
  private final UUID tenantId;
  private final UUID ticketId;
  private final BigDecimal amount;
  private PayoutStatus status;
  private final Instant createdAt;
  private Instant approvedAt;
  private Instant paidAt;
  private final long version;

  private Payout(UUID id, UUID tenantId, UUID ticketId, BigDecimal amount, PayoutStatus status, Instant createdAt, Instant approvedAt, Instant paidAt, long version) {
    this.id = id;
    this.tenantId = tenantId;
    this.ticketId = ticketId;
    this.amount = amount;
    this.status = status;
    this.createdAt = createdAt;
    this.approvedAt = approvedAt;
    this.paidAt = paidAt;
    this.version = version;
  }

  /**
   * Create a requested payout with an explicit creation timestamp (injected by caller via Clock).
   */
  public static Payout createRequested(UUID tenantId, UUID ticketId, BigDecimal amount, Instant createdAt) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(ticketId, "ticketId");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(createdAt, "createdAt");
    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be positive");
    return new Payout(UUID.randomUUID(), tenantId, ticketId, amount, PayoutStatus.REQUESTED, createdAt, null, null, 0L);
  }

  // Factory to reconstruct aggregate from persistence
  public static Payout load(UUID id, UUID tenantId, UUID ticketId, BigDecimal amount, PayoutStatus status, Instant createdAt, Instant approvedAt, Instant paidAt, long version) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(ticketId, "ticketId");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    return new Payout(id, tenantId, ticketId, amount, status, createdAt, approvedAt, paidAt, version);
  }

  public void approve(Instant when) {
    Objects.requireNonNull(when, "when");
    if (this.status != PayoutStatus.REQUESTED) throw new IllegalStateException("Only REQUESTED payouts can be approved");
    this.status = PayoutStatus.APPROVED;
    this.approvedAt = when;
  }

  public void markPaid(Instant when) {
    Objects.requireNonNull(when, "when");
    if (this.status != PayoutStatus.APPROVED && this.status != PayoutStatus.REQUESTED) {
      throw new IllegalStateException("Only APPROVED or REQUESTED payouts can be marked as PAID");
    }
    this.status = PayoutStatus.PAID;
    this.paidAt = when;
  }

  public UUID getId() { return id; }
  public UUID getTenantId() { return tenantId; }
  public UUID getTicketId() { return ticketId; }
  public BigDecimal getAmount() { return amount; }
  public PayoutStatus getStatus() { return status; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getApprovedAt() { return approvedAt; }
  public Instant getPaidAt() { return paidAt; }
  public long getVersion() { return version; }
}
