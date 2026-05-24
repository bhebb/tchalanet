package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddPromotionRuleRequest(
    @NotBlank
    @Size(max = 96)
    String ruleKey,

    @NotNull
    PromotionEvaluationPhase phase,

    @NotNull
    Integer priority,

    boolean active
) {}

