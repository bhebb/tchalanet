package com.tchalanet.server.core.pos.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.pos.domain.model.PosSessionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pos_session")
@Getter
@Setter
public class PosSessionEntity extends BaseTenantEntity {

  // ID is now inherited from BaseEntity
  // @Id
  // private UUID id;

  // tenantId is now inherited from BaseTenantEntity
  // @Column(name = "tenant_id", nullable = false)
  // private UUID tenantId;

  @Column(name = "terminal_id", nullable = false)
  private UUID terminalId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PosSessionStatus status;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;

  @Column(name = "opening_float")
  private BigDecimal openingFloat;

  @Column(name = "closing_amount")
  private BigDecimal closingAmount;

  @Column(name = "total_tickets_amount")
  private BigDecimal totalTicketsAmount;

  @Column(name = "total_payout_amount")
  private BigDecimal totalPayoutAmount;

  @Column(name = "gross_margin")
  private BigDecimal grossMargin;

  // createdAt, updatedAt, deletedAt are inherited from BaseEntity
  // @Column(name = "created_at", nullable = false)
  // private Instant createdAt;
  // @Column(name = "updated_at", nullable = false)
  // private Instant updatedAt;
}
