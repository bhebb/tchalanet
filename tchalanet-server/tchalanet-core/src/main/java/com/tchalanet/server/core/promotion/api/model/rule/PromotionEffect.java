package com.tchalanet.server.core.promotion.api.model.rule;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;

import java.math.BigDecimal;

/**
 * Public effect model consumed by sales.
 * Keep this stable. Internal config can be richer under promotionDecision.internal.
 */
public record PromotionEffect(
    PromotionRuleId ruleId,
    PromotionCampaignId campaignId,
    String ruleKey,
    PromotionEffectType type,
    String gameCode,
    int quantity,
    BigDecimal amount,
    String currency,
    String appliesTo,
    String reason,
    PromotionChoiceMode choiceMode
) {
    public PromotionEffect(
        PromotionRuleId ruleId,
        PromotionEffectType type,
        String gameCode,
        int quantity,
        BigDecimal amount,
        String currency,
        String appliesTo,
        String reason,
        PromotionChoiceMode choiceMode
    ) {
        this(ruleId, null, null, type, gameCode, quantity, amount, currency, appliesTo, reason, choiceMode);
    }

    public PromotionEffect withRuleContext(PromotionCampaignId campaignId, String ruleKey) {
        return new PromotionEffect(
            ruleId,
            campaignId,
            ruleKey,
            type,
            gameCode,
            quantity,
            amount,
            currency,
            appliesTo,
            reason,
            choiceMode
        );
    }
}
