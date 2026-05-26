package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionDecisionPort;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionDecisionJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionDecisionRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionJsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PromotionDecisionJpaAdapter implements PromotionDecisionPort {
    private final PromotionDecisionRepository repository;

    @Override
    public Optional<PromotionDecision> findById(PromotionDecisionId decisionId) {
        return repository.findById(decisionId.value())
            .map(e -> PromotionJsonMapper.fromJson(e.getId(), e.getDecisionJson()));
    }

    @Override
    public Optional<PromotionDecision> findByContextHashAndPhase(String contextHash, String phase) {
        // Left as mapping TODO if decision cache/replay is enabled.
        return Optional.empty();
    }

    @Override
    public PromotionDecision save(PromotionDecision decision) {
        var entity = new PromotionDecisionJpaEntity();
        entity.setId(decision.decisionId().value());
        entity.setDecisionStatus(decision.status().name());
        entity.setEvaluationPhase(decision.phase().name());
        entity.setEvaluatedAt(decision.evaluatedAt());
        entity.setContextHash(decision.contextHash());
        entity.setEngineVersion(decision.engineVersion());
        entity.setDecisionJson(PromotionJsonMapper.toJson(decision));
        repository.save(entity);
        return decision;
    }
}
