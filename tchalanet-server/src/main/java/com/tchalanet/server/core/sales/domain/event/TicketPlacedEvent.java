package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record TicketPlacedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    OutletId outletId,
    UUID cashierId,
    @Nullable SessionId sessionId,
    DrawId drawId,
    String gameCode,
    long stakeCents,
    String currencyCode)
    implements DomainEvent {}
