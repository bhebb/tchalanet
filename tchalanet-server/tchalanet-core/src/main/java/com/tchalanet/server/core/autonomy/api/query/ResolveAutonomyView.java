package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.common.types.enums.AutonomyLevel;

public record ResolveAutonomyView(
    boolean requiresApproval,
    String approvalRole,
    AutonomyLevel autonomyLevel,
    String resolvedFrom,
    boolean requireApprovalOnBlock,
    String reason,
    AutonomyPolicyRuleView effectiveRule
) {}
