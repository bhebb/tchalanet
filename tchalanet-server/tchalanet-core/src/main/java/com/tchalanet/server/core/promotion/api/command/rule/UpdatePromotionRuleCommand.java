package com.tchalanet.server.core.promotion.api.command.rule;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import jakarta.validation.constraints.NotNull;

public record UpdatePromotionRuleCommand(
    @NotNull TenantId tenantId,
    @NotNull PromotionCampaignId campaignId,
    @NotNull PromotionRuleId ruleId,
    String ruleKey,
    Integer priority
) implements Command<PromotionCampaignView> {}

