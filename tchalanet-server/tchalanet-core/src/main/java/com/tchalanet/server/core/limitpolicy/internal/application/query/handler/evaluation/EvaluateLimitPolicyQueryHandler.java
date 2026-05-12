package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.evaluation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.exposure.ExposureFactsReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.evaluation.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.evaluation.LimitBreachView;
import com.tchalanet.server.core.limitpolicy.application.query.model.evaluation.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.engine.LimitEvaluationEngine;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.domain.resolver.LimitResolver;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class EvaluateLimitPolicyQueryHandler
    implements QueryHandler<EvaluateLimitPolicyQuery, LimitEvaluationView> {

    private final LimitAssignmentReaderPort assignments;
    private final ExposureFactsReaderPort exposureFacts;
    private final LimitEvaluationEngine engine;
    private final LimitResolver resolver;

    @Override
    public LimitEvaluationView handle(EvaluateLimitPolicyQuery query) {
        var ctx = query.context();

        var effective = resolver.resolve(
            assignments.listActiveForTargets(ctx.scopes(), ctx.now()), ctx);

        var result = engine.evaluate(
            effective,
            exposureFacts.snapshot(ctx),
            ctx);

        return new LimitEvaluationView(
            result.outcome(),
            result.breaches().stream()
                .map(b -> new LimitBreachView(
                    b.ruleKey(),
                    b.outcome(),
                    scopeLabel(b.appliedScope()),
                    b.code(),
                    b.messageKey(),
                    BigDecimal.valueOf(b.limitValue()),
                    BigDecimal.valueOf(b.currentValue()),
                    BigDecimal.valueOf(b.deltaValue())
                ))
                .toList());
    }

    private String scopeLabel(LimitScopeRef scope) {
        return switch (scope) {
            case LimitScopeRef.TenantScope ignored -> "TENANT";
            case LimitScopeRef.DrawChannelScope ignored -> "DRAW_CHANNEL";
            case LimitScopeRef.OutletScope ignored -> "OUTLET";
            case LimitScopeRef.AgentScope ignored -> "AGENT";
        };
    }
}
