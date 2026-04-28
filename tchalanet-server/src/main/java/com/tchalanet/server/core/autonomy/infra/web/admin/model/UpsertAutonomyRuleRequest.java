package com.tchalanet.server.core.autonomy.infra.web.admin.model;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpsertAutonomyRuleRequest(
    @NotNull AutonomyTargetType targetType,
    UUID targetId,
    @NotNull AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    Long expectedVersion
) {}
