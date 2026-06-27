package com.tchalanet.server.platform.entityhistory.internal.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EntityRevisionItem(
    String revisionId,
    String entityType,
    String entityId,
    String operation,
    Instant changedAt,
    UUID changedBy,
    UUID tenantId,
    List<String> changedFields,
    List<EntityRevisionFieldChange> changedValues) {}
