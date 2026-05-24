package com.tchalanet.server.core.promotion.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.query.GetPromotionCampaignQuery;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCampaignReadPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPromotionCampaignQueryHandler implements QueryHandler<GetPromotionCampaignQuery, PromotionCampaignView> {
    private final PromotionCampaignReadPort readPort;

    @Override
    public PromotionCampaignView handle(GetPromotionCampaignQuery query) {
        return readPort.findById(query.campaignId());
    }
}

