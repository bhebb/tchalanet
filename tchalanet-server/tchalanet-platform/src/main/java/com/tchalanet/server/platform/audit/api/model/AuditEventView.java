package com.tchalanet.server.platform.audit.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

/** Immutable API projection of an audit event. */
public record AuditEventView(
    UUID id,
    TenantId tenantId,
    Instant occurredAt,
    AuditActorType actorType,
    UUID actorId,
    AuditEntityType entityType,
    String entityId,
    AuditAction action,
    String detailsJson,
    String ip,
    String userAgent) {}
