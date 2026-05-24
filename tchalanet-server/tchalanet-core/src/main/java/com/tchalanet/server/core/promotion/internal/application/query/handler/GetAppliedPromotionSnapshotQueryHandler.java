package com.tchalanet.server.core.promotion.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotView;
import com.tchalanet.server.core.promotion.api.query.GetAppliedPromotionSnapshotQuery;
import com.tchalanet.server.core.promotion.internal.application.port.out.AppliedPromotionSnapshotReadPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetAppliedPromotionSnapshotQueryHandler implements QueryHandler<GetAppliedPromotionSnapshotQuery, AppliedPromotionSnapshotView> {
    private final AppliedPromotionSnapshotReadPort readPort;

    @Override
    public AppliedPromotionSnapshotView handle(GetAppliedPromotionSnapshotQuery query) {
        return readPort.findByDecisionId(query.decisionId().value()).orElse(null);
    }
}

