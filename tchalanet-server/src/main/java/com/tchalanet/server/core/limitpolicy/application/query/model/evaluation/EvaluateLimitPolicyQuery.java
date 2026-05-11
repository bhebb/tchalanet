package com.tchalanet.server.core.limitpolicy.application.query.model.evaluation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;

public record EvaluateLimitPolicyQuery(
    LimitContext context
) implements Query<LimitEvaluationView> {}
