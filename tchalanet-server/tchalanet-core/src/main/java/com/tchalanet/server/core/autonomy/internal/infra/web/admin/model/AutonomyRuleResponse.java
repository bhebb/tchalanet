package com.tchalanet.server.core.autonomy.internal.infra.web.admin.model;

import com.tchalanet.server.core.autonomy.internal.domain.model.ApprovalRole;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;

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

