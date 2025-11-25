// common.audit.domain.model.AuditEvent
package com.tchalanet.server.common.audit.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AuditEvent {

  private final UUID id; // nullable pour les nouveaux événements

  private final UUID tenantId;
  private final Instant createdAt;
  private final UUID createdBy;

  private final AuditActorType actorType;
  private final String actorId;

  private final AuditEntityType entityType;
  private final String entityId;

  private final AuditAction action;
  private final String detailsJson; // payload JSON (stringifié)

  private final String ip;
  private final String userAgent;

  public AuditEvent(
      UUID id,
      UUID tenantId,
      Instant createdAt,
      UUID createdBy,
      AuditActorType actorType,
      String actorId,
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      String detailsJson,
      String ip,
      String userAgent) {
    this.id = id;
    this.tenantId = tenantId;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
    this.actorType = actorType;
    this.actorId = actorId;
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
    this.detailsJson = detailsJson;
    this.ip = ip;
    this.userAgent = userAgent;
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public UUID createdBy() {
    return createdBy;
  }

  public AuditActorType actorType() {
    return actorType;
  }

  public String actorId() {
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
