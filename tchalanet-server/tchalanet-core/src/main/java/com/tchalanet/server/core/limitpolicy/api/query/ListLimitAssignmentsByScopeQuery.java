package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;

public record ListLimitAssignmentsByScopeQuery(
    LimitScopeRef limitScopeRef
) implements Query<ListLimitAssignmentsView> {}
