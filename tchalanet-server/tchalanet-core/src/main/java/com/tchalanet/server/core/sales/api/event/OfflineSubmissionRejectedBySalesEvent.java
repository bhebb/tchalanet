package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record OfflineSubmissionRejectedBySalesEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSaleSubmissionId submissionId,
    String rejectionCode
) implements DomainEvent {}
