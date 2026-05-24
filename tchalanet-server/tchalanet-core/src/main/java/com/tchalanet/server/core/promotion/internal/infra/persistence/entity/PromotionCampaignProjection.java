package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record PromotionCampaignProjection(
    UUID id,
    String code,
    String name,
    String status,
    int priority,
    Instant startsAt,
    Instant endsAt,
    Instant createdAt
) {}

