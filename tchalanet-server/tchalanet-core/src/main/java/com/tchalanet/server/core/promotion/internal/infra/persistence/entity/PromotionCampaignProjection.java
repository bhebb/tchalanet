package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import java.time.Instant;
import java.util.UUID;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;

public record PromotionCampaignProjection(
    UUID id,
    String code,
    String name,
    PromotionCampaignStatus status,
    int priority,
    Instant startsAt,
    Instant endsAt,
    Instant createdAt
) {
}

