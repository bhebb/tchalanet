package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;

public record AutonomyMeta(
    boolean configured,
    boolean deleted,
    AutonomyPolicyRuleId ruleId) {}
