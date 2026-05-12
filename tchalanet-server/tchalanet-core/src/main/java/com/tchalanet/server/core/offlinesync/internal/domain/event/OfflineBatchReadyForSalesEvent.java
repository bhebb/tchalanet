package com.tchalanet.server.core.offlinesync.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record OfflineBatchReadyForSalesEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineBatchId batchId,
    int readyCount
) implements DomainEvent {}
