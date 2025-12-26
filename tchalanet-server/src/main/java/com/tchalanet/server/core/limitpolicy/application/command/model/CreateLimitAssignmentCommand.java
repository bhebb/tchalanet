package com.tchalanet.server.core.limitpolicy.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

import java.time.Instant;
import java.util.UUID;

public record CreateLimitAssignmentCommand(
    TenantId tenantId,
    UUID limitDefinitionId,
    TargetType targetType,
    UUID targetId, // agentId
    boolean enabled,
    Instant startsAt,
    Instant endsAt
) implements Command<LimitAssignment> {}
