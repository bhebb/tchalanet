package com.tchalanet.server.core.autonomy.application.query.model;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;

import java.util.UUID;

public record GetAutonomyPolicyRuleQuery(
    TenantId tenantId,
    AutonomyTargetType targetType,
    UUID targetId
) implements Query<GetAutonomyPolicyRuleResult> {}
