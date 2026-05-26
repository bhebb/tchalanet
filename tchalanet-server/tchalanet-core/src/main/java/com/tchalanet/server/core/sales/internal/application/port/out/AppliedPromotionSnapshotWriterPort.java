package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import java.time.Instant;

public interface AppliedPromotionSnapshotWriterPort {
    void createIfAbsent(TicketId ticketId, PromotionDecision decision, Instant appliedAt);
}
