package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.core.promotion.internal.application.port.out.rule.PromotionRuleReadPort;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRuleEligibilityLine;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEffectJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEligibilityLineJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEffectRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEligibilityLineRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class PromotionRuleJpaAdapter implements PromotionRuleReadPort {
    private final PromotionRuleRepository repository;
    private final PromotionRuleEffectRepository effectRepository;
    private final PromotionRuleEligibilityLineRepository eligibilityLineRepository;

    @Override
    public List<PromotionRule> findActiveRules() {
        var rules = repository.findActiveRules();
        var ruleIds = rules.stream().map(PromotionRuleJpaEntity::getId).toList();
        Map<UUID, List<PromotionRuleEffectJpaEntity>> effectsByRuleId = effectRepository.findByRuleIdIn(ruleIds)
            .stream()
            .collect(Collectors.groupingBy(PromotionRuleEffectJpaEntity::getRuleId));
        Map<UUID, List<PromotionRuleEligibilityLineJpaEntity>> eligibilityByRuleId = eligibilityLineRepository.findByRuleIdIn(ruleIds)
            .stream()
            .collect(Collectors.groupingBy(PromotionRuleEligibilityLineJpaEntity::getRuleId));

        return rules.stream()
            .map(rule -> toDomain(
                rule,
                eligibilityByRuleId.getOrDefault(rule.getId(), List.of()),
                effectsByRuleId.getOrDefault(rule.getId(), List.of())))
            .toList();
    }

    private PromotionRule toDomain(
        PromotionRuleJpaEntity rule,
        List<PromotionRuleEligibilityLineJpaEntity> eligibilityLines,
        List<PromotionRuleEffectJpaEntity> effects
    ) {
        return new PromotionRule(
            com.tchalanet.server.common.types.id.PromotionRuleId.of(rule.getId()),
            com.tchalanet.server.common.types.id.PromotionCampaignId.of(rule.getCampaignId()),
            rule.getRuleKey(),
            rule.getPriority(),
            rule.getMinPaidTotal(),
            rule.getBeforeLocalTime(),
            eligibilityLines.stream()
                .map(line -> new PromotionRuleEligibilityLine(line.getGameCode(), line.getMinCount()))
                .toList(),
            effects.stream().map(this::toEffect).toList()
        );
    }

    private PromotionEffect toEffect(PromotionRuleEffectJpaEntity effect) {
        return new PromotionEffect(
            com.tchalanet.server.common.types.id.PromotionRuleId.of(effect.getRuleId()),
            effect.getEffectType(),
            effect.getGameCode(),
            effect.getQuantity() == null ? 1 : effect.getQuantity(),
            effect.getPayoutBaseAmount() != null ? effect.getPayoutBaseAmount() : effect.getOddsOverride(),
            null,
            effect.getChargeType(),
            null,
            PromotionChoiceMode.NONE
        );
    }
}
