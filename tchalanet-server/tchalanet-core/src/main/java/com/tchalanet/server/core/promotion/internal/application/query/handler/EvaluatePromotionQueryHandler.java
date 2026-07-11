package com.tchalanet.server.core.promotion.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.promotion.api.model.*;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionQuery;
import com.tchalanet.server.core.promotion.internal.application.port.out.rule.PromotionRuleReadPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.rule.SellerTerminalPromotionEffectOverrideReadPort;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionContextHasher;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionRuleEvaluator;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionTerminalOverrideMerger;
import com.tchalanet.server.core.promotion.internal.domain.model.SellerTerminalPromotionEffectOverride;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EvaluatePromotionQueryHandler implements QueryHandler<EvaluatePromotionQuery, PromotionDecision> {
    private static final String ENGINE_VERSION = "promotionDecision-mvp-1";

    private final PromotionRuleReadPort rules;
    private final SellerTerminalPromotionEffectOverrideReadPort terminalOverrides;
    private final PromotionRuleEvaluator evaluator;
    private final PromotionContextHasher hasher;
    private final PromotionTerminalOverrideMerger overrideMerger;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public PromotionDecision handle(EvaluatePromotionQuery q) {
        var ctx = q.context();
        var hash = hasher.hash(ctx);
        var overrides = ctx.tenantId() == null || ctx.sellerTerminalId() == null
            ? List.<SellerTerminalPromotionEffectOverride>of()
            : terminalOverrides.findActiveOverrides(ctx.tenantId(), ctx.sellerTerminalId());
        var effectiveRules = overrideMerger.merge(rules.findActiveRules(), overrides);
        var effectiveHash = effectiveRules.overrideHash() == null
            ? hash
            : hash + "." + effectiveRules.overrideHash();
        var effects = effectiveRules.rules().stream()
            .flatMap(rule -> evaluator.evaluate(rule, ctx).stream())
            .toList();

        var status = effects.isEmpty() ? PromotionDecisionStatus.NOT_ELIGIBLE : PromotionDecisionStatus.APPLIED;
        var notices = new ArrayList<String>();
        if (!effects.isEmpty()) {
            notices.add("promotionDecision.applied");
        }
        if (effectiveRules.terminalOverrideApplied()) {
            notices.add("promotionDecision.terminalOverrideApplied");
        }
        var decision = new PromotionDecision(
            PromotionDecisionId.of(idGenerator.newUuid()),
            status,
            ctx.phase(),
            ctx.evaluatedAt() == null ? Instant.now(clock) : ctx.evaluatedAt(),
            effectiveHash,
            ENGINE_VERSION,
            effects,
            notices
        );

        return decision;
    }
}
