package com.tchalanet.server.core.sales.domain.event;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TerminalId;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

public record TicketCancelledEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    TerminalId terminalId,
    SessionId sessionId,
    UUID performedBy,
    String reason,
    long totalStakeCents,
    String currency
) implements DomainEvent {}
