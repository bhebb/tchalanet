package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "draw",
    indexes = {
      @Index(name = "ix_draw_tenant_date", columnList = "tenant_id, draw_date"),
      @Index(name = "ix_draw_tenant_scheduled", columnList = "tenant_id, scheduled_at"),
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
  @JoinColumn(name = "draw_channel_id", nullable = false)
  private DrawChannelJpaEntity drawChannel;

  /** Résultat attaché à ce draw (canonique global). Null tant que pas RESULTED. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "draw_result_id")
  private DrawResultJpaEntity drawResult;

  /** Jour métier (date locale du channel) — clé stable pour l'unicité. */
  @Column(name = "draw_date", nullable = false)
  private LocalDate drawDate;

  /** Instant planifié (calculé depuis draw_date + draw_time + timezone du channel). */
  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  /** Config de cutoff (secondes avant scheduled_at). */
  @Column(name = "cutoff_sec", nullable = false)
  private Integer cutoffSec;

  /** Instant de cutoff calculé à la génération: scheduled_at - cutoff_sec. */
  @Column(name = "cutoff_at", nullable = false)
  private Instant cutoffAt;

  // --------------------
  // Lifecycle timestamps (audit)
  // --------------------

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

  // --------------------
  // Status / meta
  // --------------------

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private DrawStatus status;

  @Column(name = "draw_source")
  private String drawSource;

  @Column(name = "system_generated", nullable = false)
  private Boolean systemGenerated = Boolean.TRUE;

  @Column(name = "locked", nullable = false)
  private Boolean locked = Boolean.FALSE;
}
