package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.PromotionStackingPolicy;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdatePromotionCampaignRequest(
    @Size(max = 160)
    String name,

    @Size(max = 500)
    String description,

    Instant startsAt,

    Instant endsAt,

    Integer priority,

    PromotionStackingPolicy stackingPolicy
) {}

