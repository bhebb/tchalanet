package com.tchalanet.server.core.promotion.internal.application.port.out;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;

public interface PromotionCacheEvictorPort {

    void evictAfterCampaignMutation(
        TenantId tenantId,
        PromotionCampaignId campaignId
    );

    void evictRuntimeForTenant(TenantId tenantId);

    void evictCampaignDetail(
        TenantId tenantId,
        PromotionCampaignId campaignId
    );

    void clearAdminLists();
}

