package com.tchalanet.server.core.promotion.api.model.rule;

import java.util.Map;

public record PromotionEligibilityConfigView(
    PromotionEligibilityType type,
    Map<String, Object> params
) {}
