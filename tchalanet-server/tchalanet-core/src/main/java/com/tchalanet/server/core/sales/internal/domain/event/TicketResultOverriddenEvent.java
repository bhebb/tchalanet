package com.tchalanet.server.core.sales.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketResultOverriddenEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    DrawId drawId,
    BigDecimal totalPayout,
    TicketResultStatus resultStatus,
    String reason,
    UserId performedBy)
    implements DomainEvent {}
