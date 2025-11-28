package com.tchalanet.server.session.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "pos_session")
@Audited
@Getter
@Setter
public class PosSessionJpaEntity extends BaseTenantEntity {

  @Column(name = "outlet_id", nullable = false)
  private UUID outletId;

  @Column(name = "terminal_id", nullable = false)
  private UUID terminalId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "opening_float")
  private BigDecimal openingFloat;

  @Column(name = "closing_amount")
  private BigDecimal closingAmount;

  @Column(name = "total_tickets")
  private Long totalTickets;

  @Column(name = "total_stake")
  private BigDecimal totalStake;

  @Column(name = "total_payout")
  private BigDecimal totalPayout;

  @Column(name = "gross_margin")
  private BigDecimal grossMargin;

  // meta/jsonb géré dans BaseTenantEntity via un champ dédié si nécessaire, sinon à ajouter ici
}
