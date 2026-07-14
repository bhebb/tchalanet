package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import com.tchalanet.server.core.promotion.internal.domain.model.SellerTerminalPromotionEffectOverride;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PromotionTerminalOverrideMerger {

    public EffectivePromotionRules merge(
        List<PromotionRule> tenantRules,
        List<SellerTerminalPromotionEffectOverride> terminalOverrides
    ) {
        if (terminalOverrides == null || terminalOverrides.isEmpty()) {
            return new EffectivePromotionRules(List.copyOf(tenantRules), null, false);
        }

        var effectiveRules = tenantRules.stream()
            .map(rule -> new PromotionRule(
                rule.id(),
                rule.campaignId(),
                rule.ruleKey(),
                rule.priority(),
                rule.minPaidTotal(),
                rule.beforeLocalTime(),
                rule.eligibilityLines(),
                mergeEffects(rule, terminalOverrides)
            ))
            .toList();

        return new EffectivePromotionRules(effectiveRules, hash(terminalOverrides), true);
    }

    private List<PromotionEffect> mergeEffects(
        PromotionRule rule,
        List<SellerTerminalPromotionEffectOverride> terminalOverrides
    ) {
        return rule.effects().stream()
            .map(effect -> mergeEffect(rule, effect, terminalOverrides))
            .filter(Objects::nonNull)
            .toList();
    }

    private PromotionEffect mergeEffect(
        PromotionRule rule,
        PromotionEffect effect,
        List<SellerTerminalPromotionEffectOverride> terminalOverrides
    ) {
        var override = terminalOverrides.stream()
            .filter(item -> item.matches(rule, effect.type(), effect.gameCode()))
            .findFirst();
        if (override.isEmpty()) {
            return effect;
        }
        var item = override.get();
        if (Boolean.FALSE.equals(item.effectEnabled())) {
            return null;
        }
        return effect.withTerminalOverride(
            item.quantity(),
            item.choiceMode(),
            item.generationStrategy(),
            item.regenerableBeforeConfirm(),
            item.maxRegenerationsBeforeConfirm()
        );
    }

    private String hash(List<SellerTerminalPromotionEffectOverride> overrides) {
        var input = overrides.stream()
            .map(SellerTerminalPromotionEffectOverride::signature)
            .sorted()
            .collect(java.util.stream.Collectors.joining("|"));
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
