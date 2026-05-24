package com.tchalanet.server.core.promotion.internal.application.port.out;

import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotView;
import java.util.Optional;

public interface AppliedPromotionSnapshotReadPort {
    Optional<AppliedPromotionSnapshotView> findByDecisionId(java.util.UUID decisionId);
}

