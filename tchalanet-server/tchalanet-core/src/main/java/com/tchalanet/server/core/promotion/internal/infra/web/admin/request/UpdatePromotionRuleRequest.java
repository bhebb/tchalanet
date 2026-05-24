package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import jakarta.validation.constraints.Size;

public record UpdatePromotionRuleRequest(
    @Size(max = 96)
    String ruleKey,

    PromotionEvaluationPhase phase,

    Integer priority,

    Boolean active
) {}

