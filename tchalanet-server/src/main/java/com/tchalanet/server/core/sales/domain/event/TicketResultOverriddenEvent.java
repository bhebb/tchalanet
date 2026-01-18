package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.enums.TicketResultStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketResultOverriddenEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    DrawId drawId,
    BigDecimal totalPayout,
    TicketResultStatus resultStatus,
    String reason,
    UUID performedBy)
    implements DomainEvent {}
