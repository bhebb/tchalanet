package com.tchalanet.server.core.payout.domain.model;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** Aggregate root for a Payout. */
@Getter
@Setter
public class Payout {

  private final PayoutId id;
  private final TenantId tenantId;
  private final TicketId ticketId;

  private final long amountCents;
  private final String currency;

  private PayoutStatus status;

  private final Instant createdAt;
  private Instant approvedAt;
  private Instant paidAt;

  private String rejectedReason;
  private Instant rejectedAt;

  private Payout(
      PayoutId id,
      TenantId tenantId,
      TicketId ticketId,
      long amountCents,
      String currency,
      PayoutStatus status,
      Instant createdAt,
      Instant approvedAt,
      Instant paidAt,
      String rejectedReason,
      Instant rejectedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.ticketId = ticketId;
    this.amountCents = amountCents;
    this.currency = currency;
    this.status = status;
    this.createdAt = createdAt;
    this.approvedAt = approvedAt;
    this.paidAt = paidAt;
    this.rejectedReason = rejectedReason;
    this.rejectedAt = rejectedAt;
  }

  public static Payout createRequested(
      TenantId tenantId, TicketId ticketId, long amountCents, String currency, Instant createdAt) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(ticketId, "ticketId");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(createdAt, "createdAt");
    if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be positive");

    return new Payout(
        PayoutId.random(),
        tenantId,
        ticketId,
        amountCents,
        currency,
        PayoutStatus.REQUESTED,
        createdAt,
        null,
        null,
        null,
        null);
  }

  /** Factory to reconstruct aggregate from persistence. */
  public static Payout load(
      PayoutId id,
      TenantId tenantId,
      TicketId ticketId,
      long amountCents,
      String currency,
      PayoutStatus status,
      Instant createdAt,
      Instant approvedAt,
      Instant paidAt,
      String rejectedReason,
      Instant rejectedAt,
      long version) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(ticketId, "ticketId");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be positive");

    return new Payout(
        id,
        tenantId,
        ticketId,
        amountCents,
        currency,
        status,
        createdAt,
        approvedAt,
        paidAt,
        rejectedReason,
        rejectedAt);
  }

  public void approve(Instant when) {
    Objects.requireNonNull(when, "when");
    if (status != PayoutStatus.REQUESTED) {
      throw new IllegalStateException("Only REQUESTED payouts can be approved. status=" + status);
    }
    status = PayoutStatus.APPROVED;
    approvedAt = when;
  }

  public void markPaid(Instant when, boolean allowFromRequested) {
    Objects.requireNonNull(when, "when");

    if (status == PayoutStatus.APPROVED) {
      status = PayoutStatus.PAID;
      paidAt = when;
      return;
    }
    if (allowFromRequested && status == PayoutStatus.REQUESTED) {
      status = PayoutStatus.PAID;
      paidAt = when;
      return;
    }
    throw new IllegalStateException(
        "PAID allowed only from APPROVED"
            + (allowFromRequested ? " or REQUESTED" : "")
            + ". status="
            + status);
  }

  /**
   * Reject a payout request.
   *
   * <p>Rules (V1): - Only REQUESTED can be rejected (keep it strict). - Requires a timestamp;
   * reason optional but recommended.
   */
  public void reject(String reason, Instant when) {
    Objects.requireNonNull(when, "when");
    if (status != PayoutStatus.REQUESTED) {
      throw new IllegalStateException("Only REQUESTED payouts can be rejected. status=" + status);
    }
    status = PayoutStatus.REJECTED;
    rejectedAt = when;
    rejectedReason = (reason == null || reason.isBlank()) ? null : reason.trim();
  }
}
