package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

/**
 * Domain event: a ticket previously in {@code PENDING_APPROVAL} has been
 * approved (or system-confirmed) and is now {@code APPROVED}.
 *
 * <p>This is the second half of the placement story. The pair
 * {@link TicketPlacedEvent} + {@link TicketApprovedEvent} together describe
 * how a ticket reached APPROVED state. Consumers that aggregate official sales
 * MUST subscribe to both (with deduplication on {@code ticketId}) to avoid
 * missing tickets that were approved via the workflow path.
 */
public record TicketApprovedEvent(
    // Envelope
    EventId eventId,
    int schemaVersion,
    Instant occurredAt,
    CorrelationId correlationId,

    // Subject
    TenantId tenantId,
    TicketId ticketId,

    // Approval metadata
    ApprovalRequestId approvalRequestId,
    UserId approvedBy,
    String reason
) implements com.tchalanet.server.common.event.DomainEvent {
    public static final int CURRENT_SCHEMA = 1;
}
