package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitScopeRef;

public record ListLimitAssignmentsByScopeQuery(
    LimitScopeRef limitScopeRef
) implements Query<ListLimitAssignmentsView> {}
