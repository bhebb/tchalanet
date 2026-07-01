package com.tchalanet.server.core.promotion.internal.infra.web.admin;

import com.tchalanet.server.catalog.plan.api.PlanFeatureKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.promotion.api.command.lifecycle.ActivatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.ArchivePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.DeactivatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.PausePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.query.GetPromotionCampaignQuery;
import com.tchalanet.server.core.promotion.api.query.ListPromotionCampaignsQuery;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper.PromotionCampaignAdminWebMapper;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.CreatePromotionCampaignRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.InstantiateMaryajGratisRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionCampaignRequest;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/promotions/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasPermission(null, 'promotion.read')")
@Tag(name = "Promotion • Campaigns • Admin")
public class PromotionCampaignAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final PromotionCampaignAdminWebMapper mapper;

    @GetMapping
    public ApiResponse<TchPage<PromotionCampaignView>> list(
        @CurrentContext TchRequestContext ctx,
        @TchPaging(
            allowedSort = {"createdAt", "name", "status"},
            defaultSort = {"createdAt,desc"}
        ) TchPageRequest page
    ) {
        var result = queryBus.ask(new ListPromotionCampaignsQuery(page.pageable()));
        return ApiResponse.success(result);
    }

    @GetMapping("/{campaignId}")
    public ApiResponse<PromotionCampaignView> get(
        @PathVariable UUID campaignId
    ) {
        var result = queryBus.ask(new GetPromotionCampaignQuery(PromotionCampaignId.of(campaignId)));
        return ApiResponse.success(result);
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> create(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CreatePromotionCampaignRequest request
    ) {
        var command = mapper.toCommand(ctx.effectiveTenantIdRequired(), request);
        var result = commandBus.execute(command);
        return ApiResponse.success(result);
    }

    @PutMapping("/{campaignId}")
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> update(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID campaignId,
        @Valid @RequestBody UpdatePromotionCampaignRequest request
    ) {
        var command = mapper.toCommand(ctx.effectiveTenantIdRequired(), PromotionCampaignId.of(campaignId), request);
        var result = commandBus.execute(command);
        return ApiResponse.success(result);
    }

    @PostMapping("/templates/default-maryaj-gratis/instantiate")
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> instantiateDefaultMaryajGratis(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody(required = false) InstantiateMaryajGratisRequest request
    ) {
        var result = commandBus.execute(new InstantiateDefaultMaryajGratisCommand(
            ctx.effectiveTenantIdRequired(),
            request == null ? null : request.payoutBaseAmount(),
            request == null ? null : request.quantityMode(),
            request == null ? null : request.quantity(),
            request == null ? null : request.stepPaidAmount(),
            request == null ? null : request.quantityPerStep(),
            request == null ? null : request.maxQuantity(),
            request == null ? null : request.quantityTiers(),
            request == null ? null : request.choiceMode(),
            request == null ? null : request.generationStrategy(),
            request == null ? null : request.regenerableBeforeConfirm(),
            request == null ? null : request.maxRegenerationsBeforeConfirm()
        ));
        return ApiResponse.success(result);
    }

    @PostMapping("/{campaignId}/activate")
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> activate(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID campaignId
    ) {
        var result = commandBus.execute(new ActivatePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            PromotionCampaignId.of(campaignId)
        ));
        return ApiResponse.success(result);
    }

    @PostMapping("/{campaignId}/deactivate")
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> deactivate(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID campaignId
    ) {
        var result = commandBus.execute(new DeactivatePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            PromotionCampaignId.of(campaignId)
        ));
        return ApiResponse.success(result);
    }

    @PostMapping("/{campaignId}/pause")
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> pause(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID campaignId
    ) {
        var out = commandBus.execute(new PausePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            PromotionCampaignId.of(campaignId)
        ));
        return ApiResponse.success(out);
    }

    @PostMapping("/{campaignId}/archive")
    @PreAuthorize("hasPermission(null, 'promotion.manage')")
    @RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
    public ApiResponse<PromotionCampaignView> archive(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID campaignId
    ) {
        var out = commandBus.execute(new ArchivePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            PromotionCampaignId.of(campaignId)
        ));
        return ApiResponse.success(out);
    }
}
