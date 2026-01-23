package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "draw",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_draw_tenant_channel_date",
            columnNames = {"tenantId", "draw_channel_id", "draw_date"}),
    indexes = {
      @Index(name = "ix_draw_tenant_date", columnList = "tenantId, draw_date"),
      @Index(name = "ix_draw_tenant_scheduled", columnList = "tenantId, scheduled_at"),
      @Index(name = "ix_draw_status_scheduled_at", columnList = "status, scheduled_at"),
      @Index(name = "ix_draw_status_cutoff_at", columnList = "status, cutoff_at")
      // NB: indexes partiels (WHERE deleted_at IS NULL AND locked=false) => uniquement en SQL
      // migration
    })
@Audited
@Getter
@Setter
public class DrawJpaEntity extends BaseTenantEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "draw_channel_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_draw_channel"))
  private DrawChannelJpaEntity drawChannel;

  @Column(name = "draw_date", nullable = false)
  private LocalDate drawDate;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Column(name = "cutoff_at", nullable = false)
  private Instant cutoffAt;

  @Column(name = "opened_at")
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "resulted_at")
  private Instant resultedAt;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "status", nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  private DrawStatus status;

  // raw FK column (authoritative storage of the link)
  @Column(name = "draw_result_id")
  private UUID drawResultId;

  @Column(name = "system_generated", nullable = false)
  private boolean systemGenerated = true;

  @Column(name = "locked", nullable = false)
  private boolean locked = false;

  @Column(name = "result_source", length = 16)
  @Enumerated(EnumType.STRING)
  private DrawSource resultSource;

  @Column(name = "result_override_reason")
  private String resultOverrideReason;

  @Column(name = "result_overridden_at")
  private Instant resultOverriddenAt;
}
