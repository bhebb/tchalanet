package com.tchalanet.server.core.promotion.internal.domain.model;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.PromotionRuleStatus;

import java.util.Map;

public record PromotionRule(
    PromotionRuleId id,
    PromotionCampaignId campaignId,
    String ruleKey,
    PromotionRuleStatus status,
    PromotionEvaluationPhase phase,
    Map<String, Object> eligibility,
    Map<String, Object> effect,
    String quotaKey,
    Integer maxUses
) {}
