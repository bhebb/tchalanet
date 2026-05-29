package com.tchalanet.server.core.promotion.internal.domain.model;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record PromotionRule(
    PromotionRuleId id,
    PromotionCampaignId campaignId,
    String ruleKey,
    int priority,
    BigDecimal minPaidTotal,
    LocalTime beforeLocalTime,
    List<PromotionRuleEligibilityLine> eligibilityLines,
    List<PromotionEffect> effects
) {}
