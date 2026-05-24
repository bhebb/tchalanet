package com.tchalanet.server.core.promotion.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.query.ListPromotionCampaignsQuery;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCampaignReadPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPromotionCampaignsQueryHandler implements QueryHandler<ListPromotionCampaignsQuery, TchPage<PromotionCampaignView>> {
    private final PromotionCampaignReadPort readPort;

    @Override
    public TchPage<PromotionCampaignView> handle(ListPromotionCampaignsQuery query) {
        return readPort.findCampaigns(query.pageable());
    }
}

