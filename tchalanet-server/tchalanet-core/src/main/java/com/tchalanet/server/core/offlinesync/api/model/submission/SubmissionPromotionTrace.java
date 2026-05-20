package com.tchalanet.server.core.offlinesync.api.model.submission;

import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.TicketId;

import java.time.Instant;

/**
 * Trace of the promotion exchange between {@code core.offlinesync} and {@code core.sales}.
 * A current {@code promotionAttemptId} is the key that lets returning events be matched
 * against the latest attempt and stale ones ignored.
 */
public record SubmissionPromotionTrace(
    PromotionAttemptId promotionAttemptId,
    Instant promotionRequestedAt,
    EventId lastPromotionEventId,
    TicketId createdTicketId
) {

    public static SubmissionPromotionTrace empty() {
        return new SubmissionPromotionTrace(null, null, null, null);
    }

    public SubmissionPromotionTrace withAttempt(PromotionAttemptId attemptId, Instant requestedAt) {
        if (attemptId == null) throw new IllegalArgumentException("attemptId required");
        return new SubmissionPromotionTrace(attemptId, requestedAt, lastPromotionEventId, createdTicketId);
    }

    public SubmissionPromotionTrace withReturnEvent(EventId eventId) {
        return new SubmissionPromotionTrace(promotionAttemptId, promotionRequestedAt, eventId, createdTicketId);
    }

    public SubmissionPromotionTrace withCreatedTicket(TicketId ticketId, EventId eventId) {
        if (ticketId == null) throw new IllegalArgumentException("ticketId required");
        return new SubmissionPromotionTrace(promotionAttemptId, promotionRequestedAt, eventId, ticketId);
    }

    public boolean isCurrentAttempt(PromotionAttemptId incoming) {
        return promotionAttemptId != null && promotionAttemptId.equals(incoming);
    }
}
