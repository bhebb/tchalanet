package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record OfflineBatchReceivedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSalesGrantId grantId,
    int submissionCount
) implements DomainEvent {}
