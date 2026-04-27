package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;

public record GetLimitsOverviewQuery(
    LimitTarget target
) implements Query<LimitsOverviewView> {}
