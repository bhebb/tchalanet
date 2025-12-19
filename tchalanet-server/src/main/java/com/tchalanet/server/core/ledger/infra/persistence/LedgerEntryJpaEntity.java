package com.tchalanet.server.core.ledger.infra.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntryJpaEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "ref_type", nullable = false, length = 32)
  private String refType;

  @Column(name = "ref_id", nullable = false)
  private UUID refId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "direction", nullable = false, length = 8)
  private String direction;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Version
  private Long version;

  public LedgerEntryJpaEntity() {}

  // getters/setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getTenantId() { return tenantId; }
  public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
  public String getRefType() { return refType; }
  public void setRefType(String refType) { this.refType = refType; }
  public UUID getRefId() { return refId; }
  public void setRefId(UUID refId) { this.refId = refId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }
  public Instant getOccurredAt() { return occurredAt; }
  public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}

