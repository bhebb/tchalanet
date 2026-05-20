package com.tchalanet.server.platform.audit.internal.web;

import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    UUID tenantId,
    Instant occurredAt,
    AuditActorType actorType,
    UUID actorId,
    AuditEntityType entityType,
    String entityId,
    AuditAction action,
    String details,
    String ip,
    String userAgent) {}
