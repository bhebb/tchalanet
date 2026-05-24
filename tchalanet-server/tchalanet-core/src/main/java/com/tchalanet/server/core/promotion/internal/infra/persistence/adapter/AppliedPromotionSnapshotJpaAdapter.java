package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.AppliedPromotionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotResult;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.internal.application.port.out.AppliedPromotionSnapshotPort;
import java.time.Clock;
import java.time.Instant;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.AppliedPromotionSnapshotJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.AppliedPromotionSnapshotRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionJsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AppliedPromotionSnapshotJpaAdapter implements AppliedPromotionSnapshotPort {
    private final AppliedPromotionSnapshotRepository repository;
    private final Clock clock;

    @Override
    public AppliedPromotionSnapshotResult createIfAbsent(TicketId ticketId, PromotionDecision decision) {
        var existing = repository.findByTicketIdAndPromotionDecisionId(
            ticketId.value(), decision.decisionId().value()
        );
        if (existing.isPresent()) {
            return new AppliedPromotionSnapshotResult(
                AppliedPromotionId.of(existing.get().getId()), decision.decisionId(), false
            );
        }
        var entity = new AppliedPromotionSnapshotJpaEntity();
        entity.setTicketId(ticketId.value());
        entity.setPromotionDecisionId(decision.decisionId().value());
        entity.setDecisionStatus(decision.status().name());
        entity.setAppliedAt(Instant.now(clock));
        entity.setSnapshotJson(PromotionJsonMapper.toJson(decision));
        try {
            var saved = repository.save(entity);
            return new AppliedPromotionSnapshotResult(AppliedPromotionId.of(saved.getId()), decision.decisionId(), true);
        } catch (DataIntegrityViolationException duplicate) {
            var again = repository.findByTicketIdAndPromotionDecisionId(ticketId.value(), decision.decisionId().value())
                .orElseThrow(() -> duplicate);
            return new AppliedPromotionSnapshotResult(AppliedPromotionId.of(again.getId()), decision.decisionId(), false);
        }
    }
}
