package com.tchalanet.server.core.promotion.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import jakarta.validation.constraints.NotNull;

public record EvaluatePromotionQuery(
    @NotNull PromotionEvaluationContext context
) implements Query<PromotionDecision> {}
