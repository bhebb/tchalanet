package com.tchalanet.server.core.promotion.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotView;

public record GetAppliedPromotionSnapshotQuery(
    PromotionDecisionId decisionId
) implements Query<AppliedPromotionSnapshotView> {}
