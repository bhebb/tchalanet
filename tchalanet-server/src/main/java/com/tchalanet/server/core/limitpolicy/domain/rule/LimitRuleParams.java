package com.tchalanet.server.core.limitpolicy.domain.rule;

import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import tools.jackson.databind.JsonNode;

public final class LimitRuleParams {

    private LimitRuleParams() {}

    public static long requiredLong(EffectiveLimitRule rule, String field) {
        var node = required(rule, field);

        if (!node.isNumber()) {
            throw new IllegalStateException(
                "Limit param '" + field + "' must be numeric for " + rule.ruleKey());
        }

        return node.asLong();
    }

    public static String requiredText(EffectiveLimitRule rule, String field) {
        var node = required(rule, field);

        if (!node.isTextual()) {
            throw new IllegalStateException(
                "Limit param '" + field + "' must be text for " + rule.ruleKey());
        }

        return node.asText();
    }

    public static boolean hasText(EffectiveLimitRule rule, String field) {
        if (rule.params() == null || rule.params().isNull()) {
            return false;
        }

        var node = rule.params().get(field);
        return node != null && node.isTextual() && !node.asText().isBlank();
    }

    public static boolean stringArrayContains(EffectiveLimitRule rule, String field, String value) {
        if (value == null || rule.params() == null || rule.params().isNull()) {
            return false;
        }

        var node = rule.params().get(field);

        if (node == null || !node.isArray()) {
            return false;
        }

        for (var item : node) {
            if (item != null && item.isTextual() && value.equals(item.asText())) {
                return true;
            }
        }

        return false;
    }

    private static JsonNode required(EffectiveLimitRule rule, String field) {
        if (rule.params() == null || rule.params().isNull()) {
            throw new IllegalStateException("Missing params for " + rule.ruleKey());
        }

        var node = rule.params().get(field);

        if (node == null || node.isNull()) {
            throw new IllegalStateException(
                "Missing limit param '" + field + "' for " + rule.ruleKey());
        }

        return node;
    }
}
