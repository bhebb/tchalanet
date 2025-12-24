package com.tchalanet.server.core.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.core.sales.domain.model.BetType;
import com.tchalanet.server.core.limitpolicy.domain.model.ScopeType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "draw_exposure")
public class DrawExposureJpaEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "draw_id", nullable = false)
  private UUID drawId;

  @Column(name = "scope_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ScopeType scopeType;

  @Column(name = "scope_id", nullable = false)
  private UUID scopeId;

  @Column(name = "bet_type")
  @Enumerated(EnumType.STRING)
  private BetType betType;

  @Column(name = "selection_key")
  private String selectionKey;

  @Column(name = "stake_total", nullable = false)
  private BigDecimal stakeTotal = BigDecimal.ZERO;

  @Column(name = "sales_count", nullable = false)
  private long salesCount = 0;

  @Column(name = "potential_payout_total", nullable = false)
  private BigDecimal potentialPayoutTotal = BigDecimal.ZERO;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // Composite key
  @EmbeddedId
  private DrawExposureId id;

  public static class DrawExposureId implements java.io.Serializable {
    private UUID tenantId;
    private UUID drawId;
    private ScopeType scopeType;
    private UUID scopeId;
    private BetType betType;
    private String selectionKey;

    // Getters, setters, equals, hashCode
  }

  // Getters and setters
}
