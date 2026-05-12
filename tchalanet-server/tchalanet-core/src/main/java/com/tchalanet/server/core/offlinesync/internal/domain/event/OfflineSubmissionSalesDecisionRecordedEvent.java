package com.tchalanet.server.core.offlinesync.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineRiskFlag;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineRejectReason;
import java.time.Instant;
import java.util.Set;

public record OfflineSubmissionSalesDecisionRecordedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSaleSubmissionId submissionId,
    SalesOfflineDecision decision,
    SalesOfflineRejectReason rejectReason,
    Set<OfflineRiskFlag> riskFlags,
    TicketId salesTicketId
) implements DomainEvent {}
