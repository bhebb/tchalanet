package com.tchalanet.server.core.limitpolicy.domain.model;

import java.time.Instant;
import java.util.UUID;

public record LimitAssignment(
    UUID id,
    UUID tenantId,
    UUID limitDefinitionId,
    TargetType targetType,
    UUID targetId, // null for TENANT
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    long version
) {}
