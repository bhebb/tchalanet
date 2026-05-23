package com.tchalanet.server.core.promotion.api.model;

import java.time.Instant;
import java.util.Map;

public record AppliedPromotionSnapshot(
    String ruleCode,
    int ruleVersion,
    PromotionEffectType effectType,
    String targetLineRef,
    Map<String, Object> effectSnapshot,
    Instant appliedAt
) {}
