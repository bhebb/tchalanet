package com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;

public interface PromotionCampaignWritePort {
    PromotionCampaignView create(com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand cmd);
    PromotionCampaignView update(com.tchalanet.server.core.promotion.api.command.lifecycle.UpdatePromotionCampaignCommand cmd);
    PromotionCampaignView changeStatus(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        PromotionCampaignStatus status
    );
    // Rule-level write operations (admin)
    PromotionCampaignView addRule(com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand cmd);
    PromotionCampaignView updateRule(com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand cmd);
    PromotionCampaignView deleteRule(com.tchalanet.server.core.promotion.api.command.rule.DeletePromotionRuleCommand cmd);
    PromotionCampaignView updateRuleEffects(com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand cmd);
    PromotionCampaignView updateRuleEligibility(com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand cmd);
}

