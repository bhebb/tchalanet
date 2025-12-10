package com.tchalanet.server.core.audit.application.query.model;

import com.tchalanet.server.core.audit.domain.model.AuditAction;
import com.tchalanet.server.core.audit.domain.model.AuditActorType;
import com.tchalanet.server.core.audit.domain.model.AuditEntityType;

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
    String detailsJson
) {}

