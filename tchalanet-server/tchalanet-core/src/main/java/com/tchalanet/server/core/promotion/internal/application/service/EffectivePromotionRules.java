package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import java.util.List;

public record EffectivePromotionRules(
    List<PromotionRule> rules, String overrideHash, boolean terminalOverrideApplied) {}
