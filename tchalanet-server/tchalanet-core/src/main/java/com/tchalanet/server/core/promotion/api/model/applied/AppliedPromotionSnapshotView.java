package com.tchalanet.server.core.promotion.api.model;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.Map;

public record AppliedPromotionSnapshotView(
    PromotionDecisionId decisionId,
    TicketId ticketId,
    String decisionStatus,
    Instant appliedAt,
    Map<String, Object> snapshot
) {}


