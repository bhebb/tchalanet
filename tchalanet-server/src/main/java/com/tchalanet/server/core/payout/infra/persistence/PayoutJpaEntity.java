package com.tchalanet.server.core.payout.infra.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payouts")
public class PayoutJpaEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "ticket_id", nullable = false)
  private UUID ticketId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Version
  private Long version;

  // --- Constructors ---
  public PayoutJpaEntity() {}

  // Getters and setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getTenantId() { return tenantId; }
  public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
  public UUID getTicketId() { return ticketId; }
  public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
  public Instant getPaidAt() { return paidAt; }
  public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}

