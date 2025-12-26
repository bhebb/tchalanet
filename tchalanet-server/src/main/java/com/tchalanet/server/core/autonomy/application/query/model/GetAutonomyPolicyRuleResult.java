package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

public record GetAutonomyPolicyRuleResult(
    UUID id,
    TenantId tenantId,
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt) {}
