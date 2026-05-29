package com.tchalanet.server.core.promotion.api.model.rule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PromotionRuleConfigInput(
    @NotBlank
    @Size(max = 96)
    String ruleKey,

    @NotNull
    Integer priority,

    @NotEmpty
    List<@Valid PromotionEligibilityConfigInput> eligibilityItems,

    @NotEmpty
    List<@Valid PromotionEffectConfigInput> effectItems
) {}
