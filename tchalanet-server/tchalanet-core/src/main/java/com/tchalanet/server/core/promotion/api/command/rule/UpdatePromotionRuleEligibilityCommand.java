package com.tchalanet.server.core.promotion.api.command.rule;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionEligibilityConfigInput;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdatePromotionRuleEligibilityCommand(
    @NotNull TenantId tenantId,
    @NotNull PromotionCampaignId campaignId,
    @NotNull PromotionRuleId ruleId,
    @NotNull List<PromotionEligibilityConfigInput> items
) implements Command<PromotionCampaignView> {}


