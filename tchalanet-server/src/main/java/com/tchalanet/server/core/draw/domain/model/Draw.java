package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Aggregate root: Draw
 */
public final class Draw {

  private final DrawId id;
  private final TenantId tenantId;
  private final DrawChannelView drawChannel;

  private ZonedDateTime scheduledAt;
  private ZonedDateTime cutoffAt;
  private DrawStatus status;
  private DrawSource source; // generation source
  private DrawResultId drawResultId;

  // audit timestamps
  private ZonedDateTime openedAt;
  private ZonedDateTime closedAt;
  private ZonedDateTime resultedAt;
  private ZonedDateTime settledAt;
  private ZonedDateTime canceledAt;
  private String cancelReason;

  // Result metadata
  private DrawSource resultSource; // AUTO / ADMIN_OVERRIDE
  private String resultOverrideReason;
  private ZonedDateTime resultOverriddenAt;

  // flags
  private boolean locked = false;
  private boolean systemGenerated = true;

  public Draw(
      DrawId id,
      TenantId tenantId,
      DrawChannelView drawChannel,
      ZonedDateTime scheduledAt,
      ZonedDateTime cutoffAt,
      DrawStatus status,
      DrawSource source,
      DrawResultId drawResultId) {
    this.id = Objects.requireNonNull(id);
    this.tenantId = Objects.requireNonNull(tenantId);
    this.drawChannel = Objects.requireNonNull(drawChannel);
    this.scheduledAt = Objects.requireNonNull(scheduledAt);
    this.cutoffAt = Objects.requireNonNull(cutoffAt);
    this.status = Objects.requireNonNull(status);
    this.source = Objects.requireNonNull(source);
    this.drawResultId = drawResultId;
  }

  public DrawId id() { return id; }
  public TenantId tenantId() { return tenantId; }
  public DrawChannelView drawChannel() { return drawChannel; }
  public ZonedDateTime scheduledAt() { return scheduledAt; }
  public ZonedDateTime cutoffAt() { return cutoffAt; }
  public DrawStatus status() { return status; }
  public DrawResultId drawResultId() { return drawResultId; }
  public DrawSource source() { return source; }

  public java.time.LocalDate drawDate() {
    var zone = drawChannel.timezone() == null ? java.time.ZoneId.of("UTC") : drawChannel.timezone();
    return scheduledAt == null ? null : scheduledAt.withZoneSameInstant(zone).toLocalDate();
  }

  // --- state machine methods ---

  public void open(ZonedDateTime now) {
    DrawStatusTransition.check(this.status, DrawStatus.OPEN);
    this.status = DrawStatus.OPEN;
    this.openedAt = Objects.requireNonNull(now);
  }

  public void close(ZonedDateTime now) {
    DrawStatusTransition.check(this.status, DrawStatus.CLOSED);
    this.status = DrawStatus.CLOSED;
    this.closedAt = Objects.requireNonNull(now);
  }

  /**
   * Applique un résultat au tirage.
   * Autorisé depuis CLOSED (normal) ou RESULTED (re-apply/override).
   */
  public void applyResult(DrawResultId resultId, Instant now, DrawSource resultSource) {
    // Si déjà RESULTED, on autorise le re-apply (changement de résultat)
    if (this.status != DrawStatus.RESULTED) {
      DrawStatusTransition.check(this.status, DrawStatus.RESULTED);
    }

    this.drawResultId = Objects.requireNonNull(resultId);
    this.status = DrawStatus.RESULTED;
    this.resultedAt = ZonedDateTime.ofInstant(now, scheduledAt.getZone());
    this.resultSource = resultSource;

    if (resultSource == DrawSource.ADMIN_OVERRIDE) {
      this.resultOverriddenAt = this.resultedAt;
    }
  }

  public void settle(ZonedDateTime now) {
    DrawStatusTransition.check(this.status, DrawStatus.SETTLED);
    if (this.drawResultId == null)
      throw new IllegalStateException("Cannot settle draw without result");
    this.status = DrawStatus.SETTLED;
    this.settledAt = Objects.requireNonNull(now);
  }

  public void cancel(String reason) {
    DrawStatusTransition.check(this.status, DrawStatus.CANCELED);
    this.cancelReason = reason;
    this.canceledAt = ZonedDateTime.now();
    this.status = DrawStatus.CANCELED;
  }

  public void reschedule(ZonedDateTime newScheduledAt, ZonedDateTime newCutoffAt) {
    if (status != DrawStatus.SCHEDULED) {
      throw new IllegalStateException("Can only reschedule SCHEDULED draws");
    }
    this.scheduledAt = Objects.requireNonNull(newScheduledAt);
    this.cutoffAt = Objects.requireNonNull(newCutoffAt);
  }

  public void archive() {
    this.status = DrawStatus.ARCHIVED;
  }

  // Getters
  public ZonedDateTime openedAt() { return openedAt; }
  public ZonedDateTime closedAt() { return closedAt; }
  public ZonedDateTime resultedAt() { return resultedAt; }
  public ZonedDateTime settledAt() { return settledAt; }
  public ZonedDateTime canceledAt() { return canceledAt; }
  public String cancelReason() { return cancelReason; }
  public boolean isLocked() { return locked; }
  public void setLocked(boolean locked) { this.locked = locked; }
  public boolean isSystemGenerated() { return systemGenerated; }
  public void setSystemGenerated(boolean systemGenerated) { this.systemGenerated = systemGenerated; }

  public DrawSource resultSource() { return resultSource; }
  public String resultOverrideReason() { return resultOverrideReason; }
  public ZonedDateTime resultOverriddenAt() { return resultOverriddenAt; }

  public void setOverrideReason(String reason) {
    this.resultOverrideReason = reason;
  }
}
