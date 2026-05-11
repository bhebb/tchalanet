package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.types.enums.AutonomyLevel;
import java.time.Instant;

public record AutonomyPolicyRuleView(
    String targetType,
    String targetId,
    AutonomyLevel level,
    String approvalRole,
    boolean requireApprovalOnBlock,
    boolean enabled,
    Instant startsAt,
    Instant endsAt
) {}
