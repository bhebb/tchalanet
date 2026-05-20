package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import java.util.UUID;

public record AutonomyOverviewView(
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyRule rule,
    AutonomyMeta meta) {}
