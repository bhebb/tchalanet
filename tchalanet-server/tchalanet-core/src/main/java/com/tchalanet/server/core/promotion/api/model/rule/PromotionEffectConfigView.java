package com.tchalanet.server.core.promotion.api.model.rule;

import java.util.Map;

public record PromotionEffectConfigView(
    PromotionEffectType type,
    Map<String, Object> params
) {}
