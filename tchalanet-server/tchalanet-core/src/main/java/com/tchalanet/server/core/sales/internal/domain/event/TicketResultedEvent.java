package com.tchalanet.server.core.sales.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketResultedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    BigDecimal totalPayout,
    String currency,
    OutletId sellingOutletId,
    SalesSessionId sellingSessionId
) implements DomainEvent {}
