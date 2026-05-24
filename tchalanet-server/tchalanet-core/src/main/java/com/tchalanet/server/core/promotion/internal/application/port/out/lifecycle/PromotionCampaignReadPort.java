package com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PromotionCampaignReadPort {
    TchPage<PromotionCampaignView> findCampaigns(Pageable pageable);

    Optional<PromotionCampaignView> findById(PromotionCampaignId id);

    PromotionCampaignView getRequired(@NotNull PromotionCampaignId promotionCampaignId);

}



