package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.error.PromotionErrorCodes;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityTier;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

public record InstantiateMaryajGratisRequest(
    @DecimalMin("0.01") BigDecimal payoutBaseAmount,
    PromotionQuantityMode quantityMode,
    @Min(1) @Max(10) Integer quantity,
    @DecimalMin("0.01") BigDecimal stepPaidAmount,
    @Min(1) @Max(10) Integer quantityPerStep,
    @Min(1) @Max(50) Integer maxQuantity,
    List<PromotionQuantityTier> quantityTiers,
    PromotionChoiceMode choiceMode,
    SelectionGenerationStrategy generationStrategy,
    Boolean regenerableBeforeConfirm,
    @Min(0) @Max(20) Integer maxRegenerationsBeforeConfirm) {
  public InstantiateMaryajGratisRequest {
    var effectiveChoiceMode = choiceMode == null ? PromotionChoiceMode.AUTO_GENERATE : choiceMode;
    if (effectiveChoiceMode != PromotionChoiceMode.AUTO_GENERATE
        && effectiveChoiceMode != PromotionChoiceMode.SELLER_SELECTS) {
      throw ProblemRest.of(
          PromotionErrorCodes.validation("promotion.maryaj_gratis.choice_mode_invalid"));
    }
    if (generationStrategy == SelectionGenerationStrategy.LOW_EXPOSURE_RANDOM) {
      throw ProblemRest.of(
          PromotionErrorCodes.validation(
              "promotion.maryaj_gratis.generation_strategy_unsupported"));
    }
    if (generationStrategy != null && effectiveChoiceMode != PromotionChoiceMode.AUTO_GENERATE) {
      throw ProblemRest.of(
          PromotionErrorCodes.validation(
              "promotion.maryaj_gratis.generation_strategy_requires_auto_generate"));
    }
    if (effectiveChoiceMode == PromotionChoiceMode.SELLER_SELECTS
        && Boolean.TRUE.equals(regenerableBeforeConfirm)) {
      throw ProblemRest.of(
          PromotionErrorCodes.validation(
              "promotion.maryaj_gratis.regeneration_requires_auto_generate"));
    }
    if (quantityMode == PromotionQuantityMode.PER_PAID_AMOUNT && stepPaidAmount == null) {
      throw ProblemRest.of(
          PromotionErrorCodes.validation("promotion.maryaj_gratis.step_paid_amount_required"));
    }
    if (quantityMode == PromotionQuantityMode.TIERED_PAID_AMOUNT
        && (quantityTiers == null || quantityTiers.isEmpty())) {
      throw ProblemRest.of(
          PromotionErrorCodes.validation("promotion.maryaj_gratis.quantity_tiers_required"));
    }
  }
}
