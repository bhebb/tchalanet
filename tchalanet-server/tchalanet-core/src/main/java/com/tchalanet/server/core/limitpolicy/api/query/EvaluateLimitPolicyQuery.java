package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitContext;

public record EvaluateLimitPolicyQuery(
    LimitContext context
) implements Query<LimitEvaluationView> {}
