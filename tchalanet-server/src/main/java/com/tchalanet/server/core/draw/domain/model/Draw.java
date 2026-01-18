package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawSource;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Aggregate root: Draw
 *
 * <p>Rôle du domaine - Responsabilité principale: Gérer le cycle de vie complet des tirages (draws)
 * : planification, ouverture/fermeture des ventes, rattachement des résultats, et clôture
 * financière (settlement).
 *
 * <p>Ce que l'agrégat fait - Maintient le calendrier opérationnel du tirage (issu du DrawChannel).
 * - Pilote les transitions de statut : SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED (+ CANCELED).
 * - Rattache un résultat canonique (DrawResult, global) au tirage tenant. - Stocke les timestamps
 * d'audit (opened_at, closed_at, resulted_at, settled_at, canceled_at) ; chaque transition doit
 * écrire son horodatage.
 *
 * <p>Invariants importants (à respecter dans les handlers/commands) - cutoff_at < scheduled_at
 * (fixés lors de la génération) - draw_date doit être la date locale du DrawChannel.timezone -
 * SETTLED interdit si draw.result == null - CANCELED interdit après SETTLED
 *
 * <p>Le modèle métier complet et les règles de transition sont gérés par DrawStatusTransition
 * utilitaire utilisé par les méthodes d'état de cet agrégat.
 */
public final class Draw {

  private final DrawId id;
  private final TenantId tenantId;
  private final DrawChannel drawChannel;

  private ZonedDateTime scheduledAt;
  private ZonedDateTime cutoffAt;
  private DrawStatus status;
  private final DrawSource source;
  private DrawResultId drawResultId; // référence vers le résultat global (id) — peut être null

  // audit timestamps
  private ZonedDateTime openedAt;
  private ZonedDateTime closedAt;
  private ZonedDateTime resultedAt;
  private ZonedDateTime settledAt;
  private ZonedDateTime canceledAt;
  private String cancelReason;

  // flags
  private boolean locked = false;
  private boolean systemGenerated = true;

  public Draw(
      DrawId id,
      TenantId tenantId,
      DrawChannel drawChannel,
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

  public DrawId id() {
    return id;
  }

  public TenantId tenantId() {
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

  public DrawResultId drawResultId() {
    return drawResultId;
  }

  public DrawSource source() {
    return source;
  }

  /**
   * Compute local draw date based on the drawChannel timezone. This matches the DB business slotKey
   * draw_date.
   */
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

  // Remplace l'attachement d'un objet DrawResult par l'attachement d'un identifiant DrawResultId
  public void attachResult(DrawResultId resultId, ZonedDateTime now) {
    DrawStatusTransition.check(this.status, DrawStatus.RESULTED);
    this.drawResultId = Objects.requireNonNull(resultId);
    this.status = DrawStatus.RESULTED;
    this.resultedAt = Objects.requireNonNull(now);
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

  // pour changer l’horaire via admin
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

  public ZonedDateTime openedAt() {

    return openedAt;
  }

  public ZonedDateTime closedAt() {
    return closedAt;
  }

  public ZonedDateTime resultedAt() {
    return resultedAt;
  }

  public ZonedDateTime settledAt() {
    return settledAt;
  }

  public ZonedDateTime canceledAt() {
    return canceledAt;
  }

  public String cancelReason() {
    return cancelReason;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public boolean isSystemGenerated() {
    return systemGenerated;
  }

  public void setSystemGenerated(boolean systemGenerated) {
    this.systemGenerated = systemGenerated;
  }
}
