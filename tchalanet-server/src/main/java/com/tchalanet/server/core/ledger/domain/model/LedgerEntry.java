package com.tchalanet.server.core.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LedgerEntry {
  private final UUID id;
  private final UUID tenantId;
  private final String refType;
  private final UUID refId;
  private final BigDecimal amount;
  private final String direction; // CREDIT | DEBIT
  private final Instant occurredAt;

  private LedgerEntry(UUID id, UUID tenantId, String refType, UUID refId, BigDecimal amount, String direction, Instant occurredAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.refType = refType;
    this.refId = refId;
    this.amount = amount;
    this.direction = direction;
    this.occurredAt = occurredAt;
  }

  public static LedgerEntry create(UUID tenantId, String refType, UUID refId, BigDecimal amount, String direction) {
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(refType);
    Objects.requireNonNull(refId);
    Objects.requireNonNull(amount);
    Objects.requireNonNull(direction);
    return new LedgerEntry(UUID.randomUUID(), tenantId, refType, refId, amount, direction, Instant.now());
  }

  public static LedgerEntry load(UUID id, UUID tenantId, String refType, UUID refId, BigDecimal amount, String direction, Instant occurredAt) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(refType);
    Objects.requireNonNull(refId);
    Objects.requireNonNull(amount);
    Objects.requireNonNull(direction);
    Objects.requireNonNull(occurredAt);
    return new LedgerEntry(id, tenantId, refType, refId, amount, direction, occurredAt);
  }

  // getters
  public UUID getId() { return id; }
  public UUID getTenantId() { return tenantId; }
  public String getRefType() { return refType; }
  public UUID getRefId() { return refId; }
  public BigDecimal getAmount() { return amount; }
  public String getDirection() { return direction; }
  public Instant getOccurredAt() { return occurredAt; }
}
