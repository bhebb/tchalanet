package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TicketPrintedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    UserId printedBy,
    PrintOutputFormat format,
    String reason
) implements DomainEvent {
    public static DomainEvent from(EventId eventId, Ticket saved, @NotNull PrintOutputFormat format, @Size(max = 500) String reason, Instant now) {
        return new TicketPrintedEvent(
            eventId,
            now,
            saved.identity().tenantId(),
            saved.identity().id(),
            saved.context().sellerUserId(),
            format,
            reason
        );
    }
}
