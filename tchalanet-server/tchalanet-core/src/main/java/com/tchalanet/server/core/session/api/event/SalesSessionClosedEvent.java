package com.tchalanet.server.core.session.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/** Published after a {@code SalesSession} is successfully closed and committed. */
public record SalesSessionClosedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SalesSessionId sessionId,
    OutletId outletId,
    TerminalId terminalId,
    UserId actorId,
    String reason) implements DomainEvent {}
