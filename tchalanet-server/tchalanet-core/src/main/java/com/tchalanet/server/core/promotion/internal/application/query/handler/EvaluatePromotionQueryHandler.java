package com.tchalanet.server.core.promotion.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.promotion.api.model.*;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionQuery;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionDecisionPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionRuleReadPort;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionContextHasher;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionRuleEvaluator;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EvaluatePromotionQueryHandler implements QueryHandler<EvaluatePromotionQuery, PromotionDecision> {
    private static final String ENGINE_VERSION = "promotionDecision-mvp-1";

    private final PromotionRuleReadPort rules;
    private final PromotionDecisionPort decisions;
    private final PromotionRuleEvaluator evaluator;
    private final PromotionContextHasher hasher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public PromotionDecision handle(EvaluatePromotionQuery q) {
        var ctx = q.context();
        var hash = hasher.hash(ctx);
        var effects = rules.findActiveRulesForPhase(ctx.phase()).stream()
            .flatMap(rule -> evaluator.evaluate(rule, ctx).stream())
            .toList();

        var status = effects.isEmpty() ? PromotionDecisionStatus.NOT_ELIGIBLE : PromotionDecisionStatus.APPLIED;
        var decision = new PromotionDecision(
            PromotionDecisionId.of(idGenerator.newUuid()),
            status,
            ctx.phase(),
            ctx.evaluatedAt() == null ? Instant.now(clock) : ctx.evaluatedAt(),
            hash,
            ENGINE_VERSION,
            effects,
            effects.isEmpty() ? java.util.List.of() : java.util.List.of("promotionDecision.applied")
        );

        // Persist decision on SALE_CONFIRMATION, not necessarily on preview.
        if (ctx.phase() == PromotionEvaluationPhase.SALE_CONFIRMATION) {
            return decisions.save(decision);
        }
        return decision;
    }
}
