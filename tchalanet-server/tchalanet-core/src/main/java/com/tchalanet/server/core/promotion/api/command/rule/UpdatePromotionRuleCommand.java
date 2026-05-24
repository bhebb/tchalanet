package com.tchalanet.server.core.promotion.api.command.rule;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import jakarta.validation.constraints.NotNull;

public record UpdatePromotionRuleCommand(
    @NotNull TenantId tenantId,
    @NotNull PromotionCampaignId campaignId,
    @NotNull PromotionRuleId ruleId,
    String ruleKey,
    PromotionEvaluationPhase phase,
    Integer priority,
    Boolean active
) implements Command<PromotionCampaignView> {}


