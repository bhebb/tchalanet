package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;

public record ListLimitAssignmentsByScopeQuery(
    LimitScopeQueryRef limitScopeRef
) implements Query<ListLimitAssignmentsView> {}
