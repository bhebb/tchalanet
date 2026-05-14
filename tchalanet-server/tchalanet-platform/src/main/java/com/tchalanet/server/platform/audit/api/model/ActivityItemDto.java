package com.tchalanet.server.platform.audit.api.model;

import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import java.time.Instant;
import java.util.UUID;

public record ActivityItemDto(
    UUID id,
    Instant occurredAt,
    AuditEntityType entityType,
    String entityId,
    AuditAction action,
    AuditActorType actorType,
    String actorId,
    String summary,
    String detailsJson) {}
