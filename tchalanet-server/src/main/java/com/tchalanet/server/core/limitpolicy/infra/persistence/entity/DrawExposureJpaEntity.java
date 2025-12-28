package com.tchalanet.server.core.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.ScopeType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "draw_exposure")
@IdClass(DrawExposureJpaEntity.DrawExposureId.class)
@Getter
@Setter
public class DrawExposureJpaEntity {

  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Id
  @Column(name = "draw_id", nullable = false)
  private UUID drawId;

  @Id
  @Column(name = "scope_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ScopeType scopeType;

  @Id
  @Column(name = "scope_id", nullable = false)
  private UUID scopeId;

  @Id
  @Column(name = "bet_type")
  @Enumerated(EnumType.STRING)
  private BetType betType;

  @Id
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

  // Composite key class used by @IdClass
  public static class DrawExposureId implements Serializable {
    private UUID tenantId;
    private UUID drawId;
    private ScopeType scopeType;
    private UUID scopeId;
    private BetType betType;
    private String selectionKey;

    public DrawExposureId() {}

    public DrawExposureId(
        UUID tenantId,
        UUID drawId,
        ScopeType scopeType,
        UUID scopeId,
        BetType betType,
        String selectionKey) {
      this.tenantId = tenantId;
      this.drawId = drawId;
      this.scopeType = scopeType;
      this.scopeId = scopeId;
      this.betType = betType;
      this.selectionKey = selectionKey;
    }

    // getters/setters (optional)
    public UUID getTenantId() {
      return tenantId;
    }

    public void setTenantId(UUID tenantId) {
      this.tenantId = tenantId;
    }

    public UUID getDrawId() {
      return drawId;
    }

    public void setDrawId(UUID drawId) {
      this.drawId = drawId;
    }

    public ScopeType getScopeType() {
      return scopeType;
    }

    public void setScopeType(ScopeType scopeType) {
      this.scopeType = scopeType;
    }

    public UUID getScopeId() {
      return scopeId;
    }

    public void setScopeId(UUID scopeId) {
      this.scopeId = scopeId;
    }

    public BetType getBetType() {
      return betType;
    }

    public void setBetType(BetType betType) {
      this.betType = betType;
    }

    public String getSelectionKey() {
      return selectionKey;
    }

    public void setSelectionKey(String selectionKey) {
      this.selectionKey = selectionKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DrawExposureId that = (DrawExposureId) o;
      return Objects.equals(tenantId, that.tenantId)
          && Objects.equals(drawId, that.drawId)
          && scopeType == that.scopeType
          && Objects.equals(scopeId, that.scopeId)
          && betType == that.betType
          && Objects.equals(selectionKey, that.selectionKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tenantId, drawId, scopeType, scopeId, betType, selectionKey);
    }
  }
}
