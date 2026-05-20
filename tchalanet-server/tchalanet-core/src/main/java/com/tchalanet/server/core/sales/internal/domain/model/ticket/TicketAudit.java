package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record TicketAudit(
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy
) {
    public static TicketAudit created(UserId by, Instant now) {
        return new TicketAudit(now, by, now, by);
    }

    public TicketAudit updated(UserId by, Instant now) {
        return new TicketAudit(createdAt, createdBy, now, by);
    }
}
