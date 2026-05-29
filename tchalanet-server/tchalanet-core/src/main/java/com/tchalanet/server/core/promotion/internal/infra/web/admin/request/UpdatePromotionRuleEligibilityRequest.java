package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record UpdatePromotionRuleEligibilityRequest(
    @NotEmpty
    List<@Valid Item> items
) {
    public record Item(
        @NotNull PromotionEligibilityType type,
        @NotNull Map<String, Object> params
    ) {
    }
}

