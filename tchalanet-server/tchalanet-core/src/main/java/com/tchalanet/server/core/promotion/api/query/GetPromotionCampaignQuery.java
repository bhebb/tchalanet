package com.tchalanet.server.core.promotion.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;

public record GetPromotionCampaignQuery(
    PromotionCampaignId campaignId
) implements Query<PromotionCampaignView> {}
