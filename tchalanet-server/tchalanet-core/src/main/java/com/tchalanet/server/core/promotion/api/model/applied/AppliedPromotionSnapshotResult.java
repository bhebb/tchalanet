package com.tchalanet.server.core.promotion.api.model;

import com.tchalanet.server.common.types.id.AppliedPromotionId;
import com.tchalanet.server.common.types.id.PromotionDecisionId;

public record AppliedPromotionSnapshotResult(
    AppliedPromotionId id,
    PromotionDecisionId decisionId,
    boolean created
) {}

