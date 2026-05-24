package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.core.promotion.api.model.PromotionEffectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record UpdatePromotionRuleEffectsRequest(
    @NotEmpty
    List<@Valid EffectItem> items
) {
    public record EffectItem(
        @NotNull PromotionEffectType type,
        @NotNull Map<String, Object> params
    ) {}
}

