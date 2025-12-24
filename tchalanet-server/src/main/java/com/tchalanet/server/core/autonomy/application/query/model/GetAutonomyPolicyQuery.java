package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;

import java.util.UUID;

public record GetAutonomyPolicyRuleQuery(
    UUID tenantId,
    AutonomyTargetType targetType,
    UUID targetId
) implements Query<GetAutonomyPolicyRuleResult> {}
