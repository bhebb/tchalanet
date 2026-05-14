package com.tchalanet.server.core.autonomy.internal.infra.web.admin.model;

import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;

public record AutonomyOverviewResponse(
    AutonomyTargetType targetType,
    String targetId,
    AutonomyRuleResponse rule,
    AutonomyMetaResponse meta
) {
}
