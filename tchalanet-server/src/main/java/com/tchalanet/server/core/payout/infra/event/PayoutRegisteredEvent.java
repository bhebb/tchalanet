package com.tchalanet.server.core.payout.domain.event;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutRegisteredEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    PayoutId payoutId,
    TicketId ticketId,
    SessionId sessionId,
    BigDecimal amount) implements DomainEvent {
}

