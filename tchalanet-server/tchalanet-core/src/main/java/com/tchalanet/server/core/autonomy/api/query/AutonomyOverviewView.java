package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyTargetId;

public record AutonomyOverviewView(
    AutonomyTargetType targetType,
    AutonomyTargetId targetId,
    AutonomyRule rule,
    AutonomyMeta meta) {}
