package com.tchalanet.server.core.promotion.internal.application.port.out.applied;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotResult;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;

public interface AppliedPromotionSnapshotPort {
    AppliedPromotionSnapshotResult createIfAbsent(TicketId ticketId, PromotionDecision decision);
}

