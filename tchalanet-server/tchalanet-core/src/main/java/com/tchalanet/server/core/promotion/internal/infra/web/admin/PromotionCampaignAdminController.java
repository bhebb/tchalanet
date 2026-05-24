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
import com.tchalanet.server.core.promotion.api.command.ActivatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.ArchivePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.DeactivatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.PausePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.query.GetPromotionCampaignQuery;
import com.tchalanet.server.core.promotion.api.query.ListPromotionCampaignsQuery;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper.PromotionCampaignAdminWebMapper;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.CreatePromotionCampaignRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionCampaignRequest;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import jakarta.validation.Valid;
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
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredFeature(PlanFeatureKeys.PROMOTION_CAMPAIGN_ADMIN)
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
        @PathVariable PromotionCampaignId campaignId
    ) {
        var result = queryBus.ask(new GetPromotionCampaignQuery(campaignId));
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<PromotionCampaignView> create(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CreatePromotionCampaignRequest request
    ) {
        var command = mapper.toCommand(ctx.effectiveTenantIdRequired(), request);
        var result = commandBus.execute(command);
        return ApiResponse.success(result);
    }

    @PutMapping("/{campaignId}")
    public ApiResponse<PromotionCampaignView> update(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId,
        @Valid @RequestBody UpdatePromotionCampaignRequest request
    ) {
        var command = mapper.toCommand(ctx.effectiveTenantIdRequired(), campaignId, request);
        var result = commandBus.execute(command);
        return ApiResponse.success(result);
    }

    @PostMapping("/{campaignId}/activate")
    public ApiResponse<PromotionCampaignView> activate(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId
    ) {
        var result = commandBus.execute(new ActivatePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId
        ));
        return ApiResponse.success(result);
    }

    @PostMapping("/{campaignId}/deactivate")
    public ApiResponse<PromotionCampaignView> deactivate(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId
    ) {
        var result = commandBus.execute(new DeactivatePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId
        ));
        return ApiResponse.success(result);
    }

    @PostMapping("/{campaignId}/pause")
    public ApiResponse<PromotionCampaignView> pause(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId
    ) {
        var out = commandBus.execute(new PausePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId
        ));
        return ApiResponse.success(out);
    }

    @PostMapping("/{campaignId}/archive")
    public ApiResponse<PromotionCampaignView> archive(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PromotionCampaignId campaignId
    ) {
        var out = commandBus.execute(new ArchivePromotionCampaignCommand(
            ctx.effectiveTenantIdRequired(),
            campaignId
        ));
        return ApiResponse.success(out);
    }
}
