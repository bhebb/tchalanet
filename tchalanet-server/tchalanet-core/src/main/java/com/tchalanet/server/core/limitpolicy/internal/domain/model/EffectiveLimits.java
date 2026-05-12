package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.common.types.enums.RuleKey;

import java.util.EnumMap;
import java.util.Map;

public record EffectiveLimits(
    Map<RuleKey, EffectiveLimitRule> rules
) {

    public EffectiveLimits {
        if (rules == null) {
            rules = Map.of();
        } else {
            rules = Map.copyOf(rules);
        }
    }

    public static EffectiveLimits empty() {
        return new EffectiveLimits(Map.of());
    }

    public static EffectiveLimits of(EnumMap<RuleKey, EffectiveLimitRule> rules) {
        return new EffectiveLimits(rules);
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
