package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import java.time.Instant;
import java.util.UUID;

public record PromotionCampaignProjection(
    UUID id,
    String code,
    String name,
    PromotionCampaignStatus status,
    int priority,
    Instant startsAt,
    Instant endsAt,
    Instant createdAt) {}
