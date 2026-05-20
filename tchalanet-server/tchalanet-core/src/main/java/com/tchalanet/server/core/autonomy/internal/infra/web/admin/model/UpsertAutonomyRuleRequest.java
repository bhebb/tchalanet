package com.tchalanet.server.core.autonomy.internal.infra.web.admin.model;

import com.tchalanet.server.core.autonomy.internal.domain.model.ApprovalRole;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;
import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyTargetId;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpsertAutonomyRuleRequest(
    @NotNull AutonomyTargetType targetType,
    AutonomyTargetId targetId,
    @NotNull AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt
) {}
