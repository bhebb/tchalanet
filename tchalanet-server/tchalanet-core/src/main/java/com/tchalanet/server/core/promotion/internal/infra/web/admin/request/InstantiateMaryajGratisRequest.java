package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record InstantiateMaryajGratisRequest(
    @DecimalMin("0.01")
    BigDecimal payoutBaseAmount,

    @Min(1)
    @Max(10)
    Integer quantity,

    PromotionChoiceMode choiceMode,

    SelectionGenerationStrategy generationStrategy,

    Boolean regenerableBeforeConfirm,

    @Min(0)
    @Max(20)
    Integer maxRegenerationsBeforeConfirm
) {}
