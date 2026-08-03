package com.tchalanet.server.core.draw.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Signals that a resulted tenant draw still has unsettled work after the configured attention
 * threshold. It carries aggregate counts only, never ticket selections or customer data.
 */
public record DrawSettlementAttentionEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    ResultSlotId resultSlotId,
    DrawResultId drawResultId,
    LocalDate drawDate,
    Instant resultedAt,
    long pendingTickets,
    int failedTickets)
    implements DomainEvent {}
