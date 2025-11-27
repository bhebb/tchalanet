package com.tchalanet.server.draw.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Tirage métier central Tchalanet (Haïti, US Lottery, ...). */
public record Draw(
    UUID id,
    UUID tenantId,
    UUID drawChannelId,
    String gameCode,
    Instant scheduledAt,
    int cutoffSeconds,
    DrawStatus status,
    DrawSource source,
    String resultPayloadJson,
    boolean systemGenerated,
    boolean locked) {

  // compatibility alias: cutoffSec() used in code
  public int cutoffSec() {
    return this.cutoffSeconds;
  }

  // compatibility alias: some code expects resultPayload() (string or null)
  public String resultPayload() {
    return this.resultPayloadJson;
  }

  // alias for source
  public DrawSource drawSource() {
    return this.source;
  }

  // createdBy/updatedBy may be referenced; provide nullable stubs
  public UUID createdBy() {
    return null;
  }

  public UUID updatedBy() {
    return null;
  }

  // Mutators helpers used in various services — return a new Draw with updated fields
  public Draw withScheduledAt(Instant newScheduledAt) {
    return new Draw(
        this.id,
        this.tenantId,
        this.drawChannelId,
        this.gameCode,
        newScheduledAt,
        this.cutoffSeconds,
        this.status,
        this.source,
        this.resultPayloadJson,
        this.systemGenerated,
        this.locked);
  }

  public Draw withCutoffSec(int newCutoff) {
    return new Draw(
        this.id,
        this.tenantId,
        this.drawChannelId,
        this.gameCode,
        this.scheduledAt,
        newCutoff,
        this.status,
        this.source,
        this.resultPayloadJson,
        this.systemGenerated,
        this.locked);
  }

  public Draw withStatus(DrawStatus newStatus) {
    return new Draw(
        this.id,
        this.tenantId,
        this.drawChannelId,
        this.gameCode,
        this.scheduledAt,
        this.cutoffSeconds,
        newStatus,
        this.source,
        this.resultPayloadJson,
        this.systemGenerated,
        this.locked);
  }

  public Draw withResultPayload(String payloadJson) {
    return new Draw(
        this.id,
        this.tenantId,
        this.drawChannelId,
        this.gameCode,
        this.scheduledAt,
        this.cutoffSeconds,
        this.status,
        this.source,
        payloadJson,
        this.systemGenerated,
        this.locked);
  }

  public Draw withSystemGenerated(boolean sys) {
    return new Draw(
        this.id,
        this.tenantId,
        this.drawChannelId,
        this.gameCode,
        this.scheduledAt,
        this.cutoffSeconds,
        this.status,
        this.source,
        this.resultPayloadJson,
        sys,
        this.locked);
  }

  public Draw withLocked(boolean locked) {
    return new Draw(
        this.id,
        this.tenantId,
        this.drawChannelId,
        this.gameCode,
        this.scheduledAt,
        this.cutoffSeconds,
        this.status,
        this.source,
        this.resultPayloadJson,
        this.systemGenerated,
        locked);
  }

  public Draw withUpdatedBy(UUID updatedBy) {
    // audit fields are not stored on Draw record; stub returns same Draw
    return this;
  }

  /**
   * Apply a result payload (map) to the draw and return updated Draw. Minimal implementation: store
   * toString() as JSON placeholder.
   */
  public Draw applyResult(Map<String, Object> payload, DrawSource source, UUID appliedBy) {
    String payloadJson = payload == null ? null : payload.toString();
    Draw updated = this.withResultPayload(payloadJson).withSystemGenerated(this.systemGenerated);
    // also set source
    return new Draw(
        updated.id,
        updated.tenantId,
        updated.drawChannelId,
        updated.gameCode,
        updated.scheduledAt,
        updated.cutoffSeconds,
        updated.status,
        source,
        updated.resultPayloadJson,
        updated.systemGenerated,
        updated.locked);
  }
}
