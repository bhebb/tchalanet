package com.tchalanet.server.core.promotion.internal.infra.web.admin;

import com.tchalanet.server.catalog.plan.api.PlanFeatureKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.DeletePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper.PromotionRuleAdminWebMapper;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.AddPromotionRuleRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionRuleEffectsRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionRuleEligibilityRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionRuleRequest;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/promotions/campaigns/{campaignId}/rules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredFeature(PlanFeatureKeys.PROMOTION_RULES_BASIC)
@Tag(name = "Promotion • Rules • Admin")
public class PromotionRuleAdminController {

    private final CommandBus commandBus;
    private final PromotionRuleAdminWebMapper mapper;

    @PostMapping
    public ApiResponse<PromotionCampaignView> addRule(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId,
        @Valid @RequestBody AddPromotionRuleRequest request
    ) {
        AddPromotionRuleCommand command = mapper.toCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId,
            request
        );

        var out = commandBus.execute(command);
        return ApiResponse.success(out);
    }

    @PatchMapping("/{ruleId}")
    public ApiResponse<PromotionCampaignView> updateRule(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId,
        @PathVariable PromotionRuleId ruleId,
        @Valid @RequestBody UpdatePromotionRuleRequest request
    ) {
        UpdatePromotionRuleCommand command = mapper.toCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId,
            ruleId,
            request
        );

        var out = commandBus.execute(command);
        return ApiResponse.success(out);
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<PromotionCampaignView> deleteRule(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId,
        @PathVariable PromotionRuleId ruleId
    ) {
        var out = commandBus.execute(new DeletePromotionRuleCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId,
            ruleId
        ));

        return ApiResponse.success(out);
    }

    @PatchMapping("/{ruleId}/eligibility")
    public ApiResponse<PromotionCampaignView> updateEligibility(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId,
        @PathVariable PromotionRuleId ruleId,
        @Valid @RequestBody UpdatePromotionRuleEligibilityRequest request
    ) {
        UpdatePromotionRuleEligibilityCommand command = mapper.toCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId,
            ruleId,
            request
        );

        var out = commandBus.execute(command);
        return ApiResponse.success(out);
    }

    @PatchMapping("/{ruleId}/effects")
    public ApiResponse<PromotionCampaignView> updateEffects(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId,
        @PathVariable PromotionRuleId ruleId,
        @Valid @RequestBody UpdatePromotionRuleEffectsRequest request
    ) {
        UpdatePromotionRuleEffectsCommand command = mapper.toCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId,
            ruleId,
            request
        );

        var out = commandBus.execute(command);
        return ApiResponse.success(out);
    }
}
