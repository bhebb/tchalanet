package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import java.time.Instant;

public record OfflineSubmissionSalesDecisionRecordedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSaleSubmissionId submissionId,
    OfflineSubmissionStatus finalStatus,
    TicketId ticketId,
    String rejectionCode
) implements DomainEvent {}
