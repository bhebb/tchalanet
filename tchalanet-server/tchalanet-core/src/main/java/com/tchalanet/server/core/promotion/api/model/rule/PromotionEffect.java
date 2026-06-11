package com.tchalanet.server.core.promotion.api.model.rule;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;

import java.math.BigDecimal;

/**
 * Public effect model consumed by sales.
 * Keep this stable. Internal config can be richer under promotionDecision.internal.
 * <p>
 * Selection generation fields (maryaj-gratis-auto-selection-v1):
 * {@code generationStrategy} applies when {@code choiceMode == AUTO_GENERATE};
 * V1 supports RANDOM only. {@code regenerableBeforeConfirm} allows the seller
 * to regenerate the generated selection before sale confirmation, capped by
 * {@code maxRegenerationsBeforeConfirm}.
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
    PromotionChoiceMode choiceMode,
    SelectionGenerationStrategy generationStrategy,
    boolean regenerableBeforeConfirm,
    int maxRegenerationsBeforeConfirm
) {
    public static final int DEFAULT_MAX_REGENERATIONS = 3;

    public PromotionEffect(
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
        this(ruleId, campaignId, ruleKey, type, gameCode, quantity, amount, currency, appliesTo, reason,
            choiceMode, null, false, DEFAULT_MAX_REGENERATIONS);
    }

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
        this(ruleId, null, null, type, gameCode, quantity, amount, currency, appliesTo, reason,
            choiceMode, null, false, DEFAULT_MAX_REGENERATIONS);
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
            choiceMode,
            generationStrategy,
            regenerableBeforeConfirm,
            maxRegenerationsBeforeConfirm
        );
    }
}
