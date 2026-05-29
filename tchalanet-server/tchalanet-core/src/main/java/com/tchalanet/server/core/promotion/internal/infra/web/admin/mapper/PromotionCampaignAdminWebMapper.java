package com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.UpdatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.CreatePromotionCampaignRequest;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.UpdatePromotionCampaignRequest;
import org.springframework.stereotype.Component;

@Component
public class PromotionCampaignAdminWebMapper {

    public CreatePromotionCampaignCommand toCommand(
        TenantId tenantId,
        CreatePromotionCampaignRequest request
    ) {
        return new CreatePromotionCampaignCommand(
            tenantId,
            request.name(),
            request.description(),
            request.startsAt(),
            request.endsAt(),
            request.priority(),
            request.rules()
        );
    }

    public UpdatePromotionCampaignCommand toCommand(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        UpdatePromotionCampaignRequest request
    ) {
        return new UpdatePromotionCampaignCommand(
            tenantId,
            campaignId,
            request.name(),
            request.description(),
            request.startsAt(),
            request.endsAt(),
            request.priority()
        );
    }
}
