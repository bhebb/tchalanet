package com.tchalanet.server.core.draw.domain.model;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Draw {

  private final UUID id;
  private final UUID tenantId;
  private final DrawChannel drawChannel;

  private ZonedDateTime scheduledAt;
  private ZonedDateTime cutoffAt;
  private DrawStatus status;
  private DrawSource source;
  private DrawResult result; // peut être null tant que pas RESULTED

  public Draw(
      UUID id,
      UUID tenantId,
      DrawChannel drawChannel,
      ZonedDateTime scheduledAt,
      ZonedDateTime cutoffAt,
      DrawStatus status,
      DrawSource source,
      DrawResult result) {
    this.id = Objects.requireNonNull(id);
    this.tenantId = Objects.requireNonNull(tenantId);
    this.drawChannel = Objects.requireNonNull(drawChannel);
    this.scheduledAt = Objects.requireNonNull(scheduledAt);
    this.cutoffAt = Objects.requireNonNull(cutoffAt);
    this.status = Objects.requireNonNull(status);
    this.source = Objects.requireNonNull(source);
    this.result = result;
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public DrawChannel drawChannel() {
    return drawChannel;
  }

  public ZonedDateTime scheduledAt() {
    return scheduledAt;
  }

  public ZonedDateTime cutoffAt() {
    return cutoffAt;
  }

  public DrawStatus status() {
    return status;
  }

  public DrawResult result() {
    return result;
  }

  public DrawSource source() {
    return source;
  }

  // --- state machine methods ---

  public void open() {
    DrawStatusTransition.check(this.status, DrawStatus.OPEN);
    this.status = DrawStatus.OPEN;
  }

  public void close() {
    DrawStatusTransition.check(this.status, DrawStatus.CLOSED);
    this.status = DrawStatus.CLOSED;
  }


  public void applyResult(DrawResult result) {
    DrawStatusTransition.check(this.status, DrawStatus.RESULTED);
    this.result = Objects.requireNonNull(result);
    this.status = DrawStatus.RESULTED;
  }

  public void settle() {
    DrawStatusTransition.check(this.status, DrawStatus.SETTLED);
    if (this.result == null) {
      throw new IllegalStateException("Cannot settle draw without result");
    }
    this.status = DrawStatus.SETTLED;
  }

  public void cancel(String reason) {
    DrawStatusTransition.check(this.status, DrawStatus.CANCELED);
    // TODO: stocker reason si tu veux
    this.status = DrawStatus.CANCELED;
  }

  // pour changer l’horaire via admin
  public void reschedule(ZonedDateTime newScheduledAt, ZonedDateTime newCutoffAt) {
    if (status != DrawStatus.SCHEDULED && status != DrawStatus.SCHEDULED) {
      throw new IllegalStateException("Can only reschedule PLANNED or SCHEDULED draws");
    }
    this.scheduledAt = Objects.requireNonNull(newScheduledAt);
    this.cutoffAt = Objects.requireNonNull(newCutoffAt);
  }

  public void archive() {
    this.status = DrawStatus.ARCHIVED;
  }
}
