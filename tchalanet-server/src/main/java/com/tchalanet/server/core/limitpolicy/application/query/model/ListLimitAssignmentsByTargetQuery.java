package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;

public record ListLimitAssignmentsByTargetQuery(
    LimitTarget target
) implements Query<com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsView> {}
