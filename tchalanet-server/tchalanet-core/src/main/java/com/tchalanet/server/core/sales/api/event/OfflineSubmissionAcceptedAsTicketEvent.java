package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;

public record OfflineSubmissionAcceptedAsTicketEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSaleSubmissionId submissionId,
    TicketId ticketId
) implements DomainEvent {}
