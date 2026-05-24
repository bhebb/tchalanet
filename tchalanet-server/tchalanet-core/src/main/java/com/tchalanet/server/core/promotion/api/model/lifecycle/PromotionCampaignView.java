package com.tchalanet.server.core.promotion.api.model.lifecycle;

import com.tchalanet.server.common.types.id.PromotionCampaignId;

import java.time.Instant;
import java.util.List;

public record PromotionCampaignView(
    PromotionCampaignId id,
    String code,
    String name,
    PromotionCampaignStatus status,
    int priority,
    Instant startsAt,
    Instant endsAt,
    List<PromotionRuleView> rules
) {
}

