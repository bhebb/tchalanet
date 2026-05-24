package com.tchalanet.server.core.promotion.api.command.rule;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.PromotionEligibilityConfigInput;
import java.util.List;
import jakarta.validation.constraints.NotNull;

public record AddPromotionRuleCommand(
    @NotNull TenantId tenantId,
    @NotNull PromotionCampaignId campaignId,
    @NotNull String ruleKey,
    @NotNull PromotionEvaluationPhase phase,
    @NotNull Integer priority,
    boolean active,
    List<PromotionEligibilityConfigInput> eligibilityItems,
    List<PromotionEffectConfigInput> effectItems,
    String quotaKey,
    Integer maxUses
) implements Command<PromotionCampaignView> {}


