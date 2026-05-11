package com.tchalanet.server.core.payout.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record PayoutRejectedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    PayoutId payoutId,
    TicketId ticketId,
    long amountCents,
    String currency,
    UserId rejectedBy, String reason) implements DomainEvent {}
