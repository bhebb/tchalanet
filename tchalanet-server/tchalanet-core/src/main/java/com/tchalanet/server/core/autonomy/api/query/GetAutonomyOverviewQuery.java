package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import java.util.UUID;

public record GetAutonomyOverviewQuery(AutonomyTargetType targetType, UUID targetId)
    implements Query<AutonomyOverviewView> {}
