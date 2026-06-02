package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TicketPrintedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    UserId printedBy,
    DocumentFormat outputFormat,
    PaperSize paperSize,
    String reason
) implements DomainEvent {
    public static DomainEvent from(
        EventId eventId,
        @NotNull TenantId tenantId,
        @NotNull TicketId ticketId,
        @NotNull UserId printedBy,
        @NotNull DocumentFormat outputFormat,
        @NotNull PaperSize paperSize,
        @Size(max = 500) String reason,
        Instant now
    ) {
        return new TicketPrintedEvent(
            eventId,
            now,
            tenantId,
            ticketId,
            printedBy,
            outputFormat,
            paperSize,
            reason
        );
    }
}
