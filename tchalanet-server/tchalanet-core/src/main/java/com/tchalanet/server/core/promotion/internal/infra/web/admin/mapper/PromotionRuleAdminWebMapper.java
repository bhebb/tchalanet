package com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigInput;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.AddPromotionRuleRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionRuleEffectsRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionRuleEligibilityRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionRuleRequest;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PromotionRuleAdminWebMapper {

    public AddPromotionRuleCommand toCommand(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        AddPromotionRuleRequest request
    ) {
        return new AddPromotionRuleCommand(
            tenantId,
            campaignId,
            request.ruleKey(),
            request.priority(),
            request.eligibilityItems(),
            request.effectItems()
        );
    }

    public UpdatePromotionRuleCommand toCommand(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        PromotionRuleId ruleId,
        UpdatePromotionRuleRequest request
    ) {
        return new UpdatePromotionRuleCommand(
            tenantId,
            campaignId,
            ruleId,
            request.ruleKey(),
            request.priority()
        );
    }

    public UpdatePromotionRuleEligibilityCommand toCommand(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        PromotionRuleId ruleId,
        UpdatePromotionRuleEligibilityRequest request
    ) {
        var items = request.items().stream()
            .map(i -> new PromotionEligibilityConfigInput(i.type(), i.params()))
            .toList();

        return new UpdatePromotionRuleEligibilityCommand(
            tenantId,
            campaignId,
            ruleId,
            items
        );
    }

    public UpdatePromotionRuleEffectsCommand toCommand(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        PromotionRuleId ruleId,
        UpdatePromotionRuleEffectsRequest request
    ) {
        var items = request.items().stream()
            .map(i -> new PromotionEffectConfigInput(i.type(), i.params()))
            .toList();

        return new UpdatePromotionRuleEffectsCommand(
            tenantId,
            campaignId,
            ruleId,
            items
        );
    }
}
