package com.tchalanet.server.core.promotion.api.command.rule;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectConfigInput;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdatePromotionRuleEffectsCommand(
    @NotNull TenantId tenantId,
    @NotNull PromotionCampaignId campaignId,
    @NotNull PromotionRuleId ruleId,
    @NotNull List<PromotionEffectConfigInput> items
) implements Command<PromotionCampaignView> {}


