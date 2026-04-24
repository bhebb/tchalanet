package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.query.model.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitEvaluationView;

@UseCase
public class EvaluateLimitPolicyQueryHandler implements QueryHandler<EvaluateLimitPolicyQuery, LimitEvaluationView> {

    private final com.tchalanet.server.core.limitpolicy.application.service.LimitPolicyRuntimeService runtime;

    public EvaluateLimitPolicyQueryHandler(com.tchalanet.server.core.limitpolicy.application.service.LimitPolicyRuntimeService runtime) {
        this.runtime = runtime;
    }

    @Override
    public LimitEvaluationView handle(EvaluateLimitPolicyQuery q) {
        return runtime.evaluate(q.context());
    }
}
