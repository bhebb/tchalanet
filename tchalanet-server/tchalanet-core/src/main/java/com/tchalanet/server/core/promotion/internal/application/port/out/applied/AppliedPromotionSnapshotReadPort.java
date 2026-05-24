package com.tchalanet.server.core.promotion.internal.application.port.out.applied;

import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotView;
import java.util.Optional;

public interface AppliedPromotionSnapshotReadPort {
    Optional<AppliedPromotionSnapshotView> findByDecisionId(java.util.UUID decisionId);
}


