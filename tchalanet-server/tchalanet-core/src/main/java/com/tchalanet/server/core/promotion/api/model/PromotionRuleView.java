package com.tchalanet.server.core.promotion.api.model;

import com.tchalanet.server.common.types.id.PromotionRuleId;

import java.util.List;

public record PromotionRuleView(
    PromotionRuleId id,
    String ruleKey,
    PromotionRuleStatus status,
    PromotionEvaluationPhase evaluationPhase,
    int priority,
    List<PromotionEligibilityConfigView> eligibility,
    List<PromotionEffectConfigView> effects,
    String quotaKey,
    Integer maxUses
) {}
