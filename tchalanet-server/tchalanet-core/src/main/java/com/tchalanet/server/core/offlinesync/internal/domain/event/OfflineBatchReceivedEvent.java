package com.tchalanet.server.core.offlinesync.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;

public record OfflineBatchReceivedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineBatchId batchId,
    TerminalId terminalId,
    int submissionCount
) implements DomainEvent {}
