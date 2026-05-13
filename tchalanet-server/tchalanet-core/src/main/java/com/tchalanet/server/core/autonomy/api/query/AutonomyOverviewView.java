package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import java.util.UUID;

public record AutonomyOverviewView(
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyRule rule,
    AutonomyMeta meta) {}
