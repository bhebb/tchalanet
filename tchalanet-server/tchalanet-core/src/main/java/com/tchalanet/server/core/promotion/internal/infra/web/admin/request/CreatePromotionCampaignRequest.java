package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleConfigInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

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

    @NotEmpty
    List<@Valid PromotionRuleConfigInput> rules
) {}
