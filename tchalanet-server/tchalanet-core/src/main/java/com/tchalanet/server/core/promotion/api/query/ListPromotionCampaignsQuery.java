package com.tchalanet.server.core.promotion.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import org.springframework.data.domain.Pageable;

public record ListPromotionCampaignsQuery(
    Pageable pageable
) implements Query<TchPage<PromotionCampaignView>> {}
