package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.core.autonomy.domain.model.ApprovalRole;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyLevel;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;

import java.time.Instant;
import java.util.UUID;

public record GetAutonomyPolicyRuleResult(
    UUID id,
    UUID tenantId,
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt
) {}
