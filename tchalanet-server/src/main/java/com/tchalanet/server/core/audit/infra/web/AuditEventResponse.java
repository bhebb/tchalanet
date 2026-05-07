package com.tchalanet.server.core.audit.infra.web;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.enums.AuditEntityType;
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
