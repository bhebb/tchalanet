package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotView;
import com.tchalanet.server.core.promotion.internal.application.port.out.AppliedPromotionSnapshotReadPort;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.AppliedPromotionSnapshotProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AppliedPromotionSnapshotReadJpaAdapter implements AppliedPromotionSnapshotReadPort {

    private final AppliedPromotionSnapshotProjectionRepository repository;

    @Override
    public Optional<AppliedPromotionSnapshotView> findByDecisionId(java.util.UUID decisionId) {
        return repository.findByPromotionDecisionId(decisionId)
            .map(p -> new AppliedPromotionSnapshotView(
                com.tchalanet.server.common.types.id.PromotionDecisionId.of(p.promotionDecisionId()),
                com.tchalanet.server.common.types.id.TicketId.of(p.ticketId()),
                p.decisionStatus(),
                p.appliedAt(),
                p.snapshotJson()
            ));
    }
}

