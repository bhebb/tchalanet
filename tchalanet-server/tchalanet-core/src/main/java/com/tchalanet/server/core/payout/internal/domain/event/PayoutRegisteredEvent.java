package com.tchalanet.server.core.payout.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;
import java.time.Instant;

public record PayoutRegisteredEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    PayoutId payoutId,
    TicketId ticketId,
    SalesSessionId sessionId,
    BigDecimal amount)
    implements DomainEvent {}
