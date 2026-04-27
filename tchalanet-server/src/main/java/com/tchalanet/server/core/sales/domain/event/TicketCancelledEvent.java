package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.UUID;

public record TicketCancelledEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    TerminalId terminalId,
    SessionId sessionId,
    UUID performedBy,
    String reason,
    long totalStakeCents,
    String currency,
    DrawId drawId
) implements DomainEvent {}
