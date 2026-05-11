package com.tchalanet.server.core.payout.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record PayoutPaidEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    PayoutId payoutId,
    TicketId ticketId,
    long amountCents,
    String currency,
    UserId paidBy, SalesSessionId payingSessionId, OutletId payingOutletId, TerminalId payingTerminalId) implements DomainEvent {}
