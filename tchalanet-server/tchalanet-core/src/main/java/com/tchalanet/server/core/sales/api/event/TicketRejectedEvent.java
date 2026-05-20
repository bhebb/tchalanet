package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

/**
 * Domain event: a ticket in PENDING_APPROVAL has been rejected.
 */
public record TicketRejectedEvent(
    EventId eventId,
    int schemaVersion,
    Instant occurredAt,
    CorrelationId correlationId,
    TenantId tenantId,
    TicketId ticketId,
    ApprovalRequestId approvalRequestId,
    UserId rejectedBy,
    String reason
) implements DomainEvent {
    public static final int CURRENT_SCHEMA = 1;
}

