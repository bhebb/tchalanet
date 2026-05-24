package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.PromotionStackingPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreatePromotionCampaignRequest(
    @NotBlank
    @Size(max = 160)
    String name,

    @Size(max = 500)
    String description,

    @NotNull
    Instant startsAt,

    Instant endsAt,

    @NotNull
    Integer priority,

    @NotNull
    PromotionStackingPolicy stackingPolicy
) {}

