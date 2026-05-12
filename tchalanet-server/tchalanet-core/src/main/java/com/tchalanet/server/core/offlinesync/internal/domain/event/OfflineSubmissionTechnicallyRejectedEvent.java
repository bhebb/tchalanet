package com.tchalanet.server.core.offlinesync.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineTechnicalRejectReason;
import java.time.Instant;

public record OfflineSubmissionTechnicallyRejectedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSaleSubmissionId submissionId,
    OfflineTechnicalRejectReason rejectReason
) implements DomainEvent {}
