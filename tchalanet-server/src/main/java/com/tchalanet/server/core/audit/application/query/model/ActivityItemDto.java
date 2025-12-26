package com.tchalanet.server.core.audit.application.query.model;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.enums.AuditEntityType;

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

