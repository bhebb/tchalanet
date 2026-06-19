package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;

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
    SellerTerminalId sellerTerminalId
) implements DomainEvent {
}
