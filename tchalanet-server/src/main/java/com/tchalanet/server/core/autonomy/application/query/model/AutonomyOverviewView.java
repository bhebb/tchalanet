package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;

public record AutonomyOverviewView(
    AutonomyTargetType targetType,
    AutonomyTargetId targetId,
    AutonomyRule rule,
    AutonomyMeta meta) {}
