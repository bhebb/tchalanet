package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetId;

public record GetAutonomyOverviewQuery(AutonomyTargetType targetType, AutonomyTargetId targetId)
    implements Query<AutonomyOverviewView> {}
