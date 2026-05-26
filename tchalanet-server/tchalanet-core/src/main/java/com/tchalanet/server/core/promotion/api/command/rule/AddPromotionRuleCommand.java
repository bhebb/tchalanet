package com.tchalanet.server.core.promotion.api.command.rule;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddPromotionRuleCommand(
    @NotNull TenantId tenantId,
    @NotNull PromotionCampaignId campaignId,
    @NotNull String ruleKey,
    @NotNull Integer priority,
    @NotEmpty List<@Valid PromotionEligibilityConfigInput> eligibilityItems,
    @NotEmpty List<@Valid PromotionEffectConfigInput> effectItems
) implements Command<PromotionCampaignView> {}
