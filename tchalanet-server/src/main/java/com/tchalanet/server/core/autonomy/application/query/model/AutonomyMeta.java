package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;

public record AutonomyMeta(
    boolean configured,
    boolean deleted,
    AutonomyPolicyRuleId ruleId) {}
