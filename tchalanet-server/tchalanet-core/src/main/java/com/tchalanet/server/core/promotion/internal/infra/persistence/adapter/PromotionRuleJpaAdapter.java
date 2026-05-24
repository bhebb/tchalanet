package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionRuleReadPort;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionJsonMapper;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
class PromotionRuleJpaAdapter implements PromotionRuleReadPort {
    private final PromotionRuleRepository repository;

    @Override
    public List<PromotionRule> findActiveRulesForPhase(PromotionEvaluationPhase phase) {
        return repository.findActiveRulesForPhase(phase.name()).stream()
            .map(PromotionJsonMapper::toDomain)
            .toList();
    }
}
