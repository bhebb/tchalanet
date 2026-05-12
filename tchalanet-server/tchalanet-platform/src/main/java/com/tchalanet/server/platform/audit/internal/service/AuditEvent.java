package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domaine pur: append-only audit event. - id nullable (avant persistence) - tenantId nullable (cas
 * SYSTEM global)
 */
public final class AuditEvent {

  private final UUID id; // nullable pour nouveaux événements

  private final TenantId tenantId; // nullable si event "platform/system" (optionnel)
  private final Instant occurredAt;

  // Compat / trace interne (peut être null si actor SYSTEM)
  private final UUID createdBy; // nullable

  private final AuditActorType actorType; // USER | SYSTEM | ...
  private final UUID actorId; // nullable si SYSTEM

  private final AuditEntityType entityType;
  private final String entityId; // nullable si action pas liée à une entité (rare)

  private final AuditAction action;

  /** JSON stringifié (V1). */
  private final String detailsJson;

  private final String ip; // nullable
  private final String userAgent; // nullable

  public AuditEvent(
      UUID id,
      TenantId tenantId,
      Instant occurredAt,
      UUID createdBy,
      AuditActorType actorType,
      UUID actorId,
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      String detailsJson,
      String ip,
      String userAgent) {

    this.id = id;
    this.tenantId = tenantId;

    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");

    this.actorType = Objects.requireNonNull(actorType, "actorType is required");

    // Si USER → actorId requis. Si SYSTEM → actorId peut être null.
    if (actorType == AuditActorType.USER && actorId == null) {
      throw new IllegalArgumentException("actorId is required when actorType=USER");
    }
    this.actorId = actorId;

    this.entityType = Objects.requireNonNull(entityType, "entityType is required");
    this.entityId = entityId; // optional

    this.action = Objects.requireNonNull(action, "action is required");

    this.detailsJson = (detailsJson == null || detailsJson.isBlank()) ? "{}" : detailsJson;

    this.ip = ip;
    this.userAgent = userAgent;

    // createdBy: si présent, doit être cohérent avec actor user
    if (createdBy != null && actorType != AuditActorType.USER) {
      throw new IllegalArgumentException("createdBy is only allowed when actorType=USER");
    }
    if (createdBy != null && actorId != null && !createdBy.equals(actorId)) {
      // si tu veux autoriser "createdBy != actorId" (impersonation/admin), enlève cette règle.
      throw new IllegalArgumentException("createdBy must match actorId when provided");
    }
    this.createdBy = createdBy;
  }

  /** Factory "safe" pour créer un event non persisté (id null). */
  public static AuditEvent of(
      TenantId tenantId,
      Instant occurredAt,
      AuditActorType actorType,
      UUID actorId,
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      String detailsJson,
      String ip,
      String userAgent) {

    return new AuditEvent(
        null,
        tenantId,
        occurredAt != null ? occurredAt : Instant.now(),
        actorType == AuditActorType.USER ? actorId : null,
        actorType,
        actorId,
        entityType,
        entityId,
        action,
        detailsJson,
        ip,
        userAgent);
  }

  /** Variante simple: occurredAt = now(). */
  public static AuditEvent now(
      TenantId tenantId,
      AuditActorType actorType,
      UUID actorId,
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      String detailsJson,
      String ip,
      String userAgent) {

    return of(
        tenantId,
        Instant.now(),
        actorType,
        actorId,
        entityType,
        entityId,
        action,
        detailsJson,
        ip,
        userAgent);
  }

  // Getters style record (convention Tchalanet)
  public UUID id() {
    return id;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public Instant occurredAt() {
    return occurredAt;
  }

  public UUID createdBy() {
    return createdBy;
  }

  public AuditActorType actorType() {
    return actorType;
  }

  public UUID actorId() {
    return actorId;
  }

  public AuditEntityType entityType() {
    return entityType;
  }

  public String entityId() {
    return entityId;
  }

  public AuditAction action() {
    return action;
  }

  public String detailsJson() {
    return detailsJson;
  }

  public String ip() {
    return ip;
  }

  public String userAgent() {
    return userAgent;
  }
}
