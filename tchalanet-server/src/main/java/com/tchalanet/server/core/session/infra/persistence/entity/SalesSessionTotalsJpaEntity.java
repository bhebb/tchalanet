package com.tchalanet.server.core.session.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "sales_session_totals")
@Getter
@Setter
@Audited
public class SalesSessionTotalsJpaEntity extends BaseTenantEntity {

  // Identifiant standard hérité de BaseEntity (id)

  // Colonne FK vers la table sales_session
  @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
  private UUID sessionId;

  // Relation vers la session, lecture seule côté JPA (la colonne session_id est gérée directement)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", insertable = false, updatable = false)
  private SalesSessionJpaEntity session;

  @Column(name = "total_tickets", nullable = false)
  private long totalTickets;

  @Column(name = "total_stake", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalStake;

  @Column(name = "total_payout", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalPayout;

  @Column(name = "gross_margin", nullable = false, precision = 14, scale = 2)
  private BigDecimal grossMargin;
}
