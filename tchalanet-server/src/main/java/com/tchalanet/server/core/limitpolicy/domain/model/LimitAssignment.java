package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

public record LimitAssignment(
    UUID id,
    TenantId tenantId,
    UUID limitDefinitionId,
    TargetType targetType,
    UUID targetId, // null for TENANT
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    long version) {}
