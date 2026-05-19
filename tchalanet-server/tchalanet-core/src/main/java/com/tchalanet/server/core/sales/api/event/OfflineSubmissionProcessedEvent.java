package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;

import java.time.Instant;

/**
 * Published after-commit by {@code core.sales} once it has processed a promotion attempt
 * (either by creating the {@link TicketId} or by rejecting it for a business reason).
 *
 * <p>Self-contained — carries the {@link PromotionAttemptId} so the {@code core.offlinesync}
 * return listener can detect stale results and ignore them.
 */
public record OfflineSubmissionProcessedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSubmissionId submissionId,
    PromotionAttemptId promotionAttemptId,
    Outcome outcome,
    TicketId ticketId,
    String rejectionCode,
    String rejectionReason
) implements DomainEvent {

    public enum Outcome { PROMOTED, BUSINESS_REJECTED, DUPLICATE }
}
