package com.tchalanet.server.core.promotion.api.model;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;

import java.time.Instant;
import java.util.List;

public record PromotionDecision(
    PromotionDecisionId decisionId,
    PromotionDecisionStatus status,
    PromotionEvaluationPhase phase,
    Instant evaluatedAt,
    String contextHash,
    String engineVersion,
    List<PromotionEffect> effects,
    List<String> notices
) {
    public PromotionDecision {
        effects = effects == null ? List.of() : List.copyOf(effects);
        notices = notices == null ? List.of() : List.copyOf(notices);
    }

    public boolean applied() {
        return status == PromotionDecisionStatus.APPLIED;
    }
}
