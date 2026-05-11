package com.tchalanet.server.core.autonomy.infra.web.admin.model;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;

public record AutonomyOverviewResponse(
    AutonomyTargetType targetType,
    String targetId,
    AutonomyRuleResponse rule,
    AutonomyMetaResponse meta
) {
}
