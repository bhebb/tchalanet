package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketPayoutPaidRecordedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    PayoutId payoutId,
    UserId paidBy,
    TicketSettlementStatus settlementStatus,
    /** Payout amount actually paid to the ticket holder (can be zero for non-winning tickets). */
    BigDecimal payoutAmount
) implements DomainEvent {}
