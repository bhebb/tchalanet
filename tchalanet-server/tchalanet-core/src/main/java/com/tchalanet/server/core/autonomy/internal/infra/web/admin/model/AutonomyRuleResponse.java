package com.tchalanet.server.core.autonomy.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;

import java.time.Instant;

public record AutonomyRuleResponse(
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt
) {
}

